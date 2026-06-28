package com.mli.lookgo.module.metro.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.module.metro.config.DataTaipeiApiClientConfig;
import com.mli.lookgo.module.metro.config.TDXApiClientConfig;
import com.mli.lookgo.module.metro.dao.MetroDAO;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.entity.StationFare;
import com.mli.lookgo.module.metro.model.vo.LineStationVO;
import com.mli.lookgo.module.metro.model.vo.LineTransferVO;
import com.mli.lookgo.module.metro.model.vo.LineVO;
import com.mli.lookgo.module.metro.model.vo.StationFacilityApiVO;
import com.mli.lookgo.module.metro.model.vo.StationFacilityVO;
import com.mli.lookgo.module.metro.model.vo.StationFareVO;
import com.mli.lookgo.module.metro.model.vo.StationTravelTimeVO;
import com.mli.lookgo.module.metro.model.vo.StationVO;

/**
 * 處理從外部 API 同步捷運資料到資料庫的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Service
public class MetroSyncService {

    private final TDXApiClientConfig tdxApiClientConfig;
    private final DataTaipeiApiClientConfig dataTaipeiApiClientConfig;
    private final MetroDAO metroDAO;
    private static final Logger logger = LoggerFactory.getLogger(MetroSyncService.class);

    // DataTaipei 車站設施資料集 ID
    private static final String DATA_TAIPEI_STATION_DATASET_ID = "f69dfd66-3d8e-408a-9645-c02384bda5b8";
    // 基礎會員限制 5 次/分鐘；頁間 20 秒 → 每分鐘最多 3 頁，任意 60 秒視窗最多 4 次請求
    private static final int PAGE_INTERVAL_MS = 20_000;
    // ODFare 前等待 60 秒，確保前序 4 個 sync 操作離開 60 秒滑動視窗後再開始分頁
    private static final int ODFAR_INITIAL_WAIT_MS = 60_000;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param tdxApiClientConfig
     * @param dataTaipeiApiClientConfig
     * @param metroDAO
     */
    public MetroSyncService(TDXApiClientConfig tdxApiClientConfig, DataTaipeiApiClientConfig dataTaipeiApiClientConfig,
            MetroDAO metroDAO) {
        this.tdxApiClientConfig = tdxApiClientConfig;
        this.dataTaipeiApiClientConfig = dataTaipeiApiClientConfig;
        this.metroDAO = metroDAO;
    }

    /**
     * 從 TDX API 取得路線資料，轉換後同步寫入資料庫。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLine() {
        logger.debug("開始從 TDX 同步路線資料");

        List<LineVO> tdxStationVOs = fetchAllLine();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<Line> lines = new ArrayList<>(tdxStationVOs.size());
        for (LineVO tdxLineVO : tdxStationVOs) {
            lines.add(this.toLineEntity(tdxLineVO, now));
        }

        metroDAO.upsertAllLine(lines);
        logger.debug("路線資料同步完成，共 {} 筆", lines.size());

        return new MessageVO("路線資料同步成功!");
    }

    /**
     * 從 TDX API 取得車站名稱，從 DataTaipei API 取得車站設施，合併後同步寫入資料庫。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllStation() {
        logger.debug("開始從 TDX + DataTaipei 同步車站資料");

        List<StationVO> tdxStationVOs = fetchAllStation();
        List<StationFacilityVO> dataTaipeiStationVOs = fetchAllStationFacility();

        // 以車站中文名稱為 key，建立 DataTaipei 設施資料的 Map
        Map<String, StationFacilityVO> dataTaipeiMap = dataTaipeiStationVOs.stream()
                .collect(Collectors.toMap(StationFacilityVO::getStationName, vo -> vo, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<Station> stations = new ArrayList<>(tdxStationVOs.size());
        for (StationVO tdxStationVO : tdxStationVOs) {
            StationFacilityVO dataTaipeiVO = dataTaipeiMap.get(tdxStationVO.getNameZhTw());
            stations.add(this.toStationEntity(tdxStationVO, dataTaipeiVO, now));
        }

        metroDAO.upsertAllStation(stations);
        logger.debug("車站資料同步完成，共 {} 筆", stations.size());

        return new MessageVO("車站資料同步成功!");
    }

    /**
     * 從 TDX API 取得路線車站資料，解析後同步寫入資料庫。
     * 需先同步路線和車站資料，以便解析外鍵 (line_id, station_id)。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLineStation() {
        logger.debug("開始從 TDX 同步路線車站資料");

        // 從資料庫取得現有的路線和車站，建立對應 Map 以解析外鍵
        Map<String, Short> lineLetterToIdMap = metroDAO.getAllLine().stream()
                .collect(Collectors.toMap(Line::getLetter, Line::getId, (a, b) -> a));

        Map<String, Integer> stationNameToIdMap = metroDAO.getAllStation().stream()
                .collect(Collectors.toMap(Station::getNameZhTw, Station::getId, (a, b) -> a));

        List<LineStationVO> tdxLineStationVOs = fetchAllLineStation();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<LineStation> lineStations = new ArrayList<>();
        for (LineStationVO tdxLineStationVO : tdxLineStationVOs) {
            // 只取 Direction=0 (去程) 避免重複
            if (tdxLineStationVO.getDirection() != null && tdxLineStationVO.getDirection() != 0) {
                continue;
            }

            Short lineId = lineLetterToIdMap.get(tdxLineStationVO.getLineId());
            if (lineId == null) {
                logger.warn("找不到路線代號 {} 對應的資料庫 id，跳過", tdxLineStationVO.getLineId());
                continue;
            }

            for (LineStationVO.StationDetail detail : tdxLineStationVO.getStations()) {
                Integer stationId = stationNameToIdMap.get(detail.getStationNameZhTw());

                lineStations.add(new LineStation(
                        lineId,
                        stationId,
                        detail.getSequence(),
                        detail.getStationCode(),
                        detail.getCumulativeDistance(),
                        detail.getTravelTime(),
                        now));
            }
        }

        metroDAO.upsertAllLineStation(lineStations);
        logger.debug("路線車站資料同步完成，共 {} 筆", lineStations.size());

        return new MessageVO("路線車站資料同步成功!");
    }

    /**
     * 從 TDX S2STravelTime API 取得相鄰車站行駛時間，計算累計行駛時間後更新資料庫。
     * 需先同步路線車站資料，以確保 station_code 存在。
     * 每個 LineID 只取第一筆 RouteID 避免反向重複計算。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLineStationCumulativeTime() {
        logger.debug("開始從 TDX S2STravelTime 同步累計行駛時間");

        Map<String, Short> lineLetterToIdMap = metroDAO.getAllLine().stream()
                .collect(Collectors.toMap(Line::getLetter, Line::getId, (a, b) -> a));

        List<StationTravelTimeVO> s2sTravelTimeVOs = fetchAllS2STravelTime();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 每個 LineID 只取第一筆（避免同一路線不同 RouteID 重複覆寫）
        Set<String> processedLineIds = new HashSet<>();
        List<LineStation> lineStations = new ArrayList<>();

        for (StationTravelTimeVO vo : s2sTravelTimeVOs) {
            if (!processedLineIds.add(vo.getLineId())) {
                continue;
            }

            Short lineId = lineLetterToIdMap.get(vo.getLineId());
            if (lineId == null) {
                logger.warn("找不到路線代號 {} 對應的資料庫 id，跳過", vo.getLineId());
                continue;
            }

            List<StationTravelTimeVO.TravelTimeDetail> travelTimes = vo.getTravelTimes();
            if (travelTimes == null || travelTimes.isEmpty()) {
                continue;
            }

            // 逐段累加：cumulative_time = Σ (StopTime[i] + RunTime[i])
            int cumulativeTime = 0;
            for (int i = 0; i < travelTimes.size(); i++) {
                StationTravelTimeVO.TravelTimeDetail td = travelTimes.get(i);

                LineStation fromStation = new LineStation();
                fromStation.setLineId(lineId);
                fromStation.setStationCode(td.getFromStationId());
                fromStation.setCumulativeTime((short) cumulativeTime);
                fromStation.setUpdatedAt(now);
                lineStations.add(fromStation);

                cumulativeTime += td.getStopTime() + td.getRunTime();

                // 最後一筆也補上終點站
                if (i == travelTimes.size() - 1) {
                    LineStation toStation = new LineStation();
                    toStation.setLineId(lineId);
                    toStation.setStationCode(td.getToStationId());
                    toStation.setCumulativeTime((short) cumulativeTime);
                    toStation.setUpdatedAt(now);
                    lineStations.add(toStation);
                }
            }
        }

        if (!lineStations.isEmpty()) {
            metroDAO.updateAllLineStationCumulativeTime(lineStations);
        }
        logger.debug("累計行駛時間同步完成，共更新 {} 筆", lineStations.size());

        return new MessageVO("路線車站累計行駛時間同步成功!");
    }

    /**
     * 從 TDX ODFare API 取得任意兩站間的票價，解析後同步寫入資料庫。
     * 需先同步路線車站資料，以確保 station_code 存在。
     * CitizenCode 城市優惠票 (FareClass=3) 因資料表無對應欄位，同步時跳過。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllStationFare() {
        logger.debug("開始從 TDX ODFare 同步票價資料");

        // 從 lines_stations 建立 station_code → station_id 對應 Map
        Map<String, Integer> stationCodeToIdMap = metroDAO.getAllLineStation().stream()
                .filter(ls -> ls.getStationCode() != null && ls.getStationId() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, LineStation::getStationId, (a, b) -> a));

        List<StationFareVO> odFareVOs = fetchAllODFare();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<StationFare> stationFares = new ArrayList<>();
        for (StationFareVO odFare : odFareVOs) {
            Integer fromStationId = stationCodeToIdMap.get(odFare.getOriginStationId());
            Integer toStationId = stationCodeToIdMap.get(odFare.getDestinationStationId());

            if (fromStationId == null || toStationId == null) {
                logger.warn("找不到站點 {} 或 {} 對應的資料表 id，跳過",
                        odFare.getOriginStationId(), odFare.getDestinationStationId());
                continue;
            }

            for (StationFareVO.FareDetail fareDetail : odFare.getFares()) {
                // 跳過含 CitizenCode 的城市優惠票
                if (fareDetail.getCitizenCode() != null) {
                    continue;
                }
                stationFares.add(new StationFare(
                        fromStationId,
                        toStationId,
                        fareDetail.getFareClass(),
                        fareDetail.getPrice(),
                        now));
            }
        }

        // 考量: SQL Server 每次請求最多 2100 個參數。
        // 每筆 StationFare 佔 5 個參數 → 每批最多 420 筆，使用 400 保留緩衝，分批呼叫避免超出限制。
        final int batchSize = 400;
        int totalInserted = 0;
        for (int i = 0; i < stationFares.size(); i += batchSize) {
            List<StationFare> batch = stationFares.subList(i, Math.min(i + batchSize, stationFares.size()));
            metroDAO.upsertAllStationFare(batch);
            totalInserted += batch.size();
            logger.debug("票價資料批次寫入進度：{} / {} 筆", totalInserted, stationFares.size());
        }

        logger.debug("票價資料同步完成，共 {} 筆", stationFares.size());

        return new MessageVO("票價資料同步成功!");
    }

    /**
     * 從 TDX LineTransfer API 取得路線換乘資料，解析後同步寫入資料庫。
     * 需先同步路線車站資料，以確保 station_code → lines_stations.id 對應存在。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLineTransfer() {
        logger.debug("開始從 TDX LineTransfer 同步路線換乘資料");

        // 從 lines_stations 建立 station_code → lines_stations.id 對應 Map
        Map<String, Integer> stationCodeToLineStationIdMap = metroDAO.getAllLineStation().stream()
                .filter(ls -> ls.getStationCode() != null && ls.getId() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, LineStation::getId, (a, b) -> a));

        List<LineTransferVO> lineTransferVOs = fetchAllLineTransfer();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<LineTransfer> lineTransfers = new ArrayList<>();
        for (LineTransferVO vo : lineTransferVOs) {
            Integer fromLineStationId = stationCodeToLineStationIdMap.get(vo.getFromStationId());
            Integer toLineStationId = stationCodeToLineStationIdMap.get(vo.getToStationId());

            if (fromLineStationId == null || toLineStationId == null) {
                logger.warn("找不到站點 {} 或 {} 對應的 lines_stations.id，跳過",
                        vo.getFromStationId(), vo.getToStationId());
                continue;
            }

            lineTransfers.add(new LineTransfer(
                    fromLineStationId,
                    toLineStationId,
                    vo.getTransferTime().shortValue(),
                    now));
        }

        if (!lineTransfers.isEmpty()) {
            metroDAO.upsertAllLineTransfer(lineTransfers);
        }
        logger.debug("路線換乘資料同步完成，共 {} 筆", lineTransfers.size());

        return new MessageVO("路線換乘資料同步成功!");
    }

    /**
     * 檢查資料庫中是否已有捷運路線資料，用於判斷是否為首次部署。
     *
     * @return 若路線資料表為空則回傳 true
     */
    public boolean isMetroDataEmpty() {
        return metroDAO.getAllLine().isEmpty();
    }

    /**
     * 將 TDX 回傳的 LineVO 轉換為資料庫的 Line 實體。
     *
     * @param vo  TDX 回傳的路線資料
     * @param now
     * @return Line
     */
    private Line toLineEntity(LineVO tdxLineVO, LocalDateTime updatedAt) {
        return new Line(
                tdxLineVO.getLetter(),
                tdxLineVO.getNameZhTw(),
                tdxLineVO.getNameEn(),
                tdxLineVO.getColor(),
                updatedAt);
    }

    /**
     * 合併 TDX 車站名稱和 DataTaipei 車站設施資料，轉換為資料庫的 Station 實體。
     *
     * @param tdxStationVO TDX 回傳的車站名稱資料
     * @param dataTaipeiVO DataTaipei 回傳的車站設施資料（可能為 null，表示該站無對應設施資料）
     * @param updatedAt    當下 UTC 時間
     * @return Station
     */
    private Station toStationEntity(StationVO tdxStationVO, StationFacilityVO dataTaipeiVO,
            LocalDateTime updatedAt) {
        return new Station(
                tdxStationVO.getNameZhTw(),
                tdxStationVO.getNameEn(),
                dataTaipeiVO != null ? dataTaipeiVO.getAtm() : null,
                dataTaipeiVO != null ? dataTaipeiVO.getNursingRoom() : null,
                dataTaipeiVO != null ? dataTaipeiVO.getDiaperTable() : null,
                dataTaipeiVO != null ? dataTaipeiVO.getChargingStation() : null,
                dataTaipeiVO != null ? dataTaipeiVO.getTicketMachine() : null,
                dataTaipeiVO != null ? dataTaipeiVO.getDrinkingWater() : null,
                dataTaipeiVO != null ? dataTaipeiVO.getRestroom() : null,
                dataTaipeiVO != null ? dataTaipeiVO.getElevator() : null,
                dataTaipeiVO != null ? dataTaipeiVO.getEscalator() : null,
                updatedAt);
    }

    // ----- TDX API 請求定義 -----
    // $top值為數字字串，因 queryParams() 參數只接受 MultiValueMap<String, String>，無法同時處理 多種型別值

    private List<LineVO> fetchAllLine() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "2000"));

        return tdxApiClientConfig.sendGetRequest("/Line", new ParameterizedTypeReference<List<LineVO>>() {
        }, params);
    }

    private List<StationVO> fetchAllStation() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "2000"));

        return tdxApiClientConfig.sendGetRequest("/Station", new ParameterizedTypeReference<List<StationVO>>() {
        }, params);
    }

    private List<LineStationVO> fetchAllLineStation() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "2000"));

        return tdxApiClientConfig.sendGetRequest("/StationOfLine",
                new ParameterizedTypeReference<List<LineStationVO>>() {
                }, params);
    }

    private List<StationTravelTimeVO> fetchAllS2STravelTime() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "2000"));

        return tdxApiClientConfig.sendGetRequest("/S2STravelTime",
                new ParameterizedTypeReference<List<StationTravelTimeVO>>() {
                }, params);
    }

    private List<LineTransferVO> fetchAllLineTransfer() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("$format", "JSON", "$top", "1000"));

        return tdxApiClientConfig.sendGetRequest("/LineTransfer",
                new ParameterizedTypeReference<List<LineTransferVO>>() {
                }, params);
    }

    private List<StationFareVO> fetchAllODFare() {
        final int pageSize = 1000;
        int skip = 0;
        List<StationFareVO> allResults = new ArrayList<>();

        logger.debug("[ODFare] 開始初始等待 {} 秒，清空前序 sync 操作的速率限制視窗", ODFAR_INITIAL_WAIT_MS / 1000);
        try {
            Thread.sleep(ODFAR_INITIAL_WAIT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("[ODFare] 初始等待被中斷，提前結束");
            return allResults;
        }
        logger.debug("[ODFare] 初始等待完成，開始分頁請求 (pageSize={}, pageIntervalSec={})",
                pageSize, PAGE_INTERVAL_MS / 1000);

        while (true) {
            int pageNumber = (skip / pageSize) + 1;
            logger.debug("[ODFare] 發送第 {} 頁請求 ($skip={}, $top={})", pageNumber, skip, pageSize);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.setAll(Map.of("$format", "JSON", "$top", String.valueOf(pageSize), "$skip", String.valueOf(skip)));

            List<StationFareVO> page = tdxApiClientConfig.sendGetRequest("/ODFare",
                    new ParameterizedTypeReference<List<StationFareVO>>() {
                    }, params);

            if (page == null || page.isEmpty()) {
                logger.debug("[ODFare] 第 {} 頁回傳空資料，結束分頁", pageNumber);
                break;
            }

            allResults.addAll(page);
            logger.debug("[ODFare] 第 {} 頁完成，本頁 {} 筆，累計 {} 筆", pageNumber, page.size(), allResults.size());

            if (page.size() < pageSize) {
                logger.debug("[ODFare] 第 {} 頁筆數 ({}) < pageSize ({})，已是最後一頁", pageNumber, page.size(), pageSize);
                break;
            }

            skip += pageSize;
            logger.debug("[ODFare] 等待 {} 秒後請求第 {} 頁", PAGE_INTERVAL_MS / 1000, pageNumber + 1);
            try {
                Thread.sleep(PAGE_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                logger.warn("[ODFare] 頁間等待被中斷，提前結束，已累計 {} 筆", allResults.size());
                break;
            }
        }

        logger.debug("[ODFare] 分頁請求全部完成，共取得 {} 筆 OD 票價資料", allResults.size());
        return allResults;
    }

    // ----- DataTaipei API 請求定義 -----

    private List<StationFacilityVO> fetchAllStationFacility() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("scope", "resourceAquire", "limit", "1000"));

        StationFacilityApiVO response = dataTaipeiApiClientConfig.sendGetRequest(
                DATA_TAIPEI_STATION_DATASET_ID, StationFacilityApiVO.class, params);

        if (response == null) {
            return Collections.emptyList();
        }

        return response.getAllStation();
    }
}
