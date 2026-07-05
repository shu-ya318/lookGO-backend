package com.mli.lookgo.module.metro.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // DataTaipei: 車站設施資料集 id
    private static final String DATA_TAIPEI_STATION_DATASET_id = "f69dfd66-3d8e-408a-9645-c02384bda5b8";

    // TDX: 基礎會員限制 5 次/分鐘；頁間 20 秒 → 每分鐘最多 3 頁，任意 60 秒視窗最多 4 次請求
    private static final int PAGE_INTERVAL_MS = 20_000;
    // TDX: 票價 (StationFare) API 前等待 60 秒再開始分頁請求，避免超出基礎會員限制
    private static final int STATION_FARE_INITIAL_WAIT_MS = 60_000;

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
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        List<Line> lines = new ArrayList<>(tdxStationVOs.size());
        for (LineVO tdxLineVO : tdxStationVOs) {
            lines.add(this.toLineEntity(tdxLineVO, currentTime));
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
        logger.debug("開始從 TDX + DataTaipei 同步請求車站資料");

        List<StationVO> tdxStationVOs = fetchAllStation();
        // 診斷用: 印出 TDX 回傳中名稱含「北車站」的原始資料與字串長度，確認 fetch 端是否拿到台北車站、名稱是否含不可見字元
        tdxStationVOs.stream()
                .filter(vo -> vo.getNameZhTw() != null && vo.getNameZhTw().contains("北車站"))
                .forEach(vo -> logger.debug("[診斷] TDX 回傳車站: StationID={}, name=\"{}\", nameLength={}",
                        vo.getStationSequence(), vo.getNameZhTw(), vo.getNameZhTw().length()));
        logger.debug("[診斷] TDX /Station 共回傳 {} 筆", tdxStationVOs.size());

        List<StationFacilityVO> dataTaipeiStationVOs = fetchAllStationFacility();

        // 以車站中文名稱為 key，建立 DataTaipei 設施資料的 Map，方便查詢
        Map<String, StationFacilityVO> dataTaipeiMap = dataTaipeiStationVOs.stream()
                .collect(Collectors.toMap(StationFacilityVO::getStationName, vo -> vo,
                        // 車站不重複: 若有相同 key，保留第一次出現的值
                        (existingValue, newValue) -> existingValue));

        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        // TDX /Station 是依「路線」而非「實體車站」回傳，換乘站會有多筆重複站名（如台北車站同時是 R10、BL12）；
        // 同一批次內對同一個 name_zh_tw 送出多筆 MERGE 會互相覆蓋、導致該站最終未被寫入，故先在 Java 端以站名去重，只保留第一筆
        Map<String, Station> stationByName = new HashMap<>();
        for (StationVO tdxStationVO : tdxStationVOs) {
            // 透過 Key (車站名稱) 比對並取得 DataTaipei 的設施資料
            StationFacilityVO dataTaipeiVO = dataTaipeiMap.get(tdxStationVO.getNameZhTw());
            stationByName.putIfAbsent(tdxStationVO.getNameZhTw(),
                    this.toStationEntity(tdxStationVO, dataTaipeiVO, currentTime));
        }
        List<Station> stations = new ArrayList<>(stationByName.values());

        // 加上每筆綁定 13 個參數，總參數數易超過 SQL Server 單次請求 2100 上限，故設定 batchSize = 150 分批 Upsert
        // 寫入
        final int batchSize = 150;
        int totalUpserted = 0;
        for (int i = 0; i < stations.size(); i += batchSize) {
            List<Station> batch = stations.subList(i, Math.min(i + batchSize, stations.size()));
            metroDAO.upsertAllStation(batch);
            totalUpserted += batch.size();
            logger.debug("車站資料批次寫入進度：{} / {} 筆", totalUpserted, stations.size());
        }
        logger.debug("車站資料同步完成，共 {} 筆", stations.size());

        return new MessageVO("車站資料同步成功!");
    }

    /**
     * 從 TDX API 取得路線車站資料，再同步寫入資料庫。
     * 需先同步路線和車站資料，以便取得外鍵 (line_id, station_id)。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLineStation() {
        logger.debug("開始從 TDX 同步路線車站資料");

        // 1. 取得資料庫所有路線，建立「路線代號 -> 路線 id」的對照 Map。例如:{"R" -> 2}
        Map<String, Short> lineLetterToIdMap = metroDAO.getAllLine().stream()
                .collect(Collectors.toMap(Line::getLetter, Line::getId, (existingValue, newValue) -> existingValue));

        // 2. 取得資料庫所有車站，建立「車站中文名稱 -> 車站 id」的對照 Map。例如:{"淡水" -> 101, "紅樹林" -> 102}
        Map<String, Integer> stationNameToIdMap = metroDAO.getAllStation().stream()
                .collect(Collectors.toMap(Station::getNameZhTw, Station::getId,
                        (existingValue, newValue) -> existingValue));

        // 3. 向 TDX API 取得所有路線與車站的對照資料
        List<LineStationVO> tdxLineStationVOs = fetchAllLineStation();
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        List<LineStation> lineStations = new ArrayList<>();
        for (LineStationVO tdxLineStationVO : tdxLineStationVOs) {
            // 4. 只處理 Direction=0 (去程) 的路線資料，略過相同路線的回程資料
            if (tdxLineStationVO.getDirection() != null && tdxLineStationVO.getDirection() != 0) {
                continue;
            }

            // 5. 根據 API 的路線代號查找對應 lineId，找不到則警告並略過。
            Short lineId = lineLetterToIdMap.get(tdxLineStationVO.getLineId());
            if (lineId == null) {
                logger.warn("找不到路線代號 {} 對應的資料庫 id，略過", tdxLineStationVO.getLineId());
                continue;
            }

            for (LineStationVO.StationDetail detail : tdxLineStationVO.getStations()) {
                // 6. 依車站中文名稱找出對應的資料庫 stationId。
                Integer stationId = stationNameToIdMap.get(detail.getStationNameZhTw());

                // 7. 將 lineId, stationId 與其他車站資訊封裝成 LineStation Entity 再加入
                lineStations.add(new LineStation(
                        lineId,
                        stationId,
                        detail.getSequence(),
                        detail.getStationCode(),
                        detail.getCumulativeDistance(),
                        detail.getTravelTime(),
                        currentTime));
            }
        }

        // 每筆綁定 7 個參數，總參數數易超過 SQL Server 單次請求 2100 上限，故設定 batchSize = 250 分批 Upsert 寫入
        final int batchSize = 250;
        int totalUpserted = 0;
        for (int i = 0; i < lineStations.size(); i += batchSize) {
            List<LineStation> batch = lineStations.subList(i, Math.min(i + batchSize, lineStations.size()));
            metroDAO.upsertAllLineStation(batch);
            totalUpserted += batch.size();
            logger.debug("路線車站資料批次寫入進度：{} / {} 筆", totalUpserted, lineStations.size());
        }
        logger.debug("路線車站資料同步完成，共 {} 筆", lineStations.size());

        return new MessageVO("路線車站資料同步成功!");
    }

    /**
     * 從 TDX StationTravelTime API 取得相鄰車站行駛時間，計算累計行駛時間後更新資料庫。
     * 需先同步路線車站資料，以確保 station_code 存在。
     * 同一 LineId 可能對應多筆路線資料（同路線的去回程重複資料，或像中和新蘆線這種 Y
     * 字分岔路線、蘆洲／迴龍兩條支線共用同一個 LineId 的資料），故以路線實際途經的車站代碼集合
     * （而非僅 LineId）判斷是否為重複路線，避免分岔支線被誤判為重複而整條略過。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLineStationCumulativeTime() {
        logger.debug("開始從 TDX StationTravelTime API 同步累計行駛時間");

        // 1. 取得資料庫所有路線，建立「路線代號 -> 路線 id」的對照 Map。例如:{"R" -> 2}
        Map<String, Short> lineLetterToIdMap = metroDAO.getAllLine().stream()
                .collect(Collectors.toMap(Line::getLetter, Line::getId, (existingValue, newValue) -> existingValue));

        // 2. 從 TDX API 取得所有車站間的行駛與停站時間資料
        List<StationTravelTimeVO> s2sTravelTimeVOs = fetchAllStationTravelTime();
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        // 3. 用於追蹤已處理的路線（以途經車站集合為鍵），避免同路線去回程重複覆寫
        Set<String> processedRouteKeys = new HashSet<>();
        List<LineStation> lineStations = new ArrayList<>();

        for (StationTravelTimeVO vo : s2sTravelTimeVOs) {
            List<StationTravelTimeVO.TravelTimeDetail> travelTimes = vo.getTravelTimes();
            if (travelTimes == null || travelTimes.isEmpty()) {
                continue;
            }

            // 4. 以「LineId + 途經車站代碼集合（排序後去向無關）」建立路線識別鍵，
            // 去回程資料途經車站相同會得到同一把鍵而被略過；分岔支線因途經車站不同（如蘆洲支線含
            // O50~O54、迴龍支線含 O13~O21）會得到不同的鍵，兩條支線才都會被處理到
            Set<String> stationCodesInRoute = travelTimes.stream()
                    .flatMap(detail -> Stream.of(detail.getFromStationId(), detail.getToStationId()))
                    .collect(Collectors.toCollection(TreeSet::new));
            String routeKey = vo.getLineId() + ":" + String.join(",", stationCodesInRoute);

            if (!processedRouteKeys.add(routeKey)) {
                continue;
            }

            // 5. 依路線代號查出資料庫對應的 lineId，找不到則警告並跳過
            Short lineId = lineLetterToIdMap.get(vo.getLineId());
            if (lineId == null) {
                logger.warn("找不到路線代號 {} 對應的資料庫 id，跳過", vo.getLineId());
                continue;
            }

            // 6. 逐站累加行駛秒數，起點站設為 0 秒（例如：淡水站為 0 秒）
            int cumulativeTime = 0;
            for (int i = 0; i < travelTimes.size(); i++) {
                StationTravelTimeVO.TravelTimeDetail td = travelTimes.get(i);

                // 7. 建立目前車站（起點端）的累計時間記錄（例如：建立淡水站 R28 的記錄，時間為 0 秒）
                LineStation fromStation = new LineStation();
                fromStation.setLineId(lineId);
                fromStation.setStationCode(td.getFromStationId());
                fromStation.setCumulativeTime((short) cumulativeTime);
                fromStation.setUpdatedAt(currentTime);
                lineStations.add(fromStation);

                // 8. 累加目前區段的停站與行駛秒數（例如：累加淡水停站 30 秒 + 淡水到紅樹林行車 130 秒，累加後為 160 秒）
                cumulativeTime += td.getStopTime() + td.getRunTime();

                // 9. 若已是最後一筆站間記錄，則補上最後終點站的累計記錄（例如：建立紅樹林站 R27 的記錄，時間為 160 秒）
                if (i == travelTimes.size() - 1) {
                    LineStation toStation = new LineStation();
                    toStation.setLineId(lineId);
                    toStation.setStationCode(td.getToStationId());
                    toStation.setCumulativeTime((short) cumulativeTime);
                    toStation.setUpdatedAt(currentTime);
                    lineStations.add(toStation);
                }
            }
        }

        // 10. 若有成功解析出累計時間資料，則整批更新寫入資料庫
        if (!lineStations.isEmpty()) {
            metroDAO.updateAllLineStationCumulativeTime(lineStations);
        }
        logger.debug("累計行駛時間同步完成，共更新 {} 筆", lineStations.size());

        return new MessageVO("路線車站累計行駛時間同步成功!");
    }

    /**
     * 從 TDX 票價 (StationFare) API 取得任意兩站間的票價，解析後同步寫入資料庫。
     * 需先同步路線車站資料，以確保 station_code 存在。
     * CitizenCode 城市優惠票 (FareClass=3) 因資料表無對應欄位，同步時跳過。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllStationFare() {
        logger.debug("開始從 TDX 票價 (StationFare) 同步票價資料");

        // 1. 取得資料庫中「車站代碼 -> 車站 id」的對照 Map。例如:{"R28" -> 101, "BL12" -> 105}
        Map<String, Integer> stationCodeToIdMap = metroDAO.getAllLineStation().stream()
                .filter(ls -> ls.getStationCode() != null && ls.getStationId() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, LineStation::getStationId,
                        (existingValue, newValue) -> existingValue));

        // 2. 從 TDX API 取得所有車站配對的票價資料
        List<StationFareVO> stationFareVOs = fetchAllStationFare();
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        List<StationFare> stationFares = new ArrayList<>();
        for (StationFareVO stationFareVO : stationFareVOs) {
            // 3. 依 API 起迄站代碼查找對應資料庫 id（例如：將起點 "R28" 對應至 101，終點 "BL12" 對應至 105）
            Integer fromStationId = stationCodeToIdMap.get(stationFareVO.getOriginStationId());
            Integer toStationId = stationCodeToIdMap.get(stationFareVO.getDestinationStationId());

            // 4. 若起迄站中任意一站找不到資料庫 id，則警告並略過該筆票價資料
            if (fromStationId == null || toStationId == null) {
                logger.warn("找不到車站 {} 或 {} 對應的資料表 id，跳過",
                        stationFareVO.getOriginStationId(), stationFareVO.getDestinationStationId());
                continue;
            }

            for (StationFareVO.FareDetail fareDetail : stationFareVO.getFares()) {
                // 5. 跳過含有 CitizenCode 的市民優惠票種
                if (fareDetail.getCitizenCode() != null) {
                    continue;
                }

                // 6. 將起迄站 id、票種與票價封裝成 StationFare Entity 並加入清單（例如：淡水至台北車站，普通票 50 元）
                stationFares.add(new StationFare(
                        fromStationId,
                        toStationId,
                        fareDetail.getFareClass(),
                        fareDetail.getPrice(),
                        currentTime));
            }
        }

        // 7. 因 SQL Server 單次請求有參數數量限制，故設定 batchSize = 400 進行分批 Upsert 寫入
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

        // 1. 取得資料庫所有路線車站關聯，建立「車站代碼 -> 路線車站關聯表 id」的對照 Map。例如:{"BL12" -> 30, "R10" -> 12}
        Map<String, Integer> stationCodeToLineStationIdMap = metroDAO.getAllLineStation().stream()
                .filter(ls -> ls.getStationCode() != null && ls.getId() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, LineStation::getId,
                        (existingValue, newValue) -> existingValue));

        // 2. 從 TDX API 取得所有轉乘站的路線間換乘（步行）時間資料
        List<LineTransferVO> lineTransferVOs = fetchAllLineTransfer();
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        List<LineTransfer> lineTransfers = new ArrayList<>();
        for (LineTransferVO vo : lineTransferVOs) {
            // 3. 依 API 的起迄站代碼查找對應的路線車站關聯 id（例如：將板南線台北車站 "BL12" 與淡水信義線台北車站 "R10" 轉乘對應至關聯 id）
            Integer fromLineStationId = stationCodeToLineStationIdMap.get(vo.getFromStationId());
            Integer toLineStationId = stationCodeToLineStationIdMap.get(vo.getToStationId());

            // 4. 若起迄站中任意一站找不到路線車站關聯 id，則警告並略過該筆轉乘資料
            if (fromLineStationId == null || toLineStationId == null) {
                logger.warn("找不到車站 {} 或 {} 對應的 lines_stations.id，跳過",
                        vo.getFromStationId(), vo.getToStationId());
                continue;
            }

            // 5. 將起迄路線車站關聯 id 與轉乘步行時間（秒）封裝成 LineTransfer Entity（例如：台北車站板南線到淡水信義線轉乘需 180 秒）
            lineTransfers.add(new LineTransfer(
                    fromLineStationId,
                    toLineStationId,
                    vo.getTransferTime().shortValue(),
                    currentTime));
        }

        // 6. 每筆綁定 4 個參數，總參數數易超過 SQL Server 單次請求 2100 上限，故設定 batchSize = 500 分批 Upsert
        // 寫入
        final int batchSize = 500;
        int totalUpserted = 0;
        for (int i = 0; i < lineTransfers.size(); i += batchSize) {
            List<LineTransfer> batch = lineTransfers.subList(i, Math.min(i + batchSize, lineTransfers.size()));
            metroDAO.upsertAllLineTransfer(batch);
            totalUpserted += batch.size();
            logger.debug("路線換乘資料批次寫入進度：{} / {} 筆", totalUpserted, lineTransfers.size());
        }
        logger.debug("路線換乘資料同步完成，共 {} 筆", lineTransfers.size());

        return new MessageVO("路線換乘資料同步成功!");
    }

    /**
     * 檢查資料庫中是否已有捷運路線資料，用於判斷是否首次部署或容器初始化作為排程啟動依據。
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

    private List<StationTravelTimeVO> fetchAllStationTravelTime() {
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

    private List<StationFareVO> fetchAllStationFare() {
        final int pageSize = 1000;
        int skip = 0;
        List<StationFareVO> allResults = new ArrayList<>();

        logger.debug("[StationFare] 開始初始等待 {} 秒，清空前幾個 sync 請求的速率限制", STATION_FARE_INITIAL_WAIT_MS / 1000);
        try {
            Thread.sleep(STATION_FARE_INITIAL_WAIT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("[StationFare] 初始等待被中斷，提前結束");
            return allResults;
        }

        logger.debug("[StationFare] 初始等待完成，開始分頁請求 (pageSize={}, pageIntervalSec={})",
                pageSize, PAGE_INTERVAL_MS / 1000);

        while (true) {
            int pageNumber = (skip / pageSize) + 1;
            logger.debug("[StationFare] 發送第 {} 頁請求 ($skip={}, $top={})", pageNumber, skip, pageSize);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.setAll(Map.of("$format", "JSON", "$top", String.valueOf(pageSize), "$skip", String.valueOf(skip)));

            List<StationFareVO> page = tdxApiClientConfig.sendGetRequest("/ODFare",
                    new ParameterizedTypeReference<List<StationFareVO>>() {
                    }, params);

            if (page == null || page.isEmpty()) {
                logger.debug("[StationFare] 第 {} 頁回傳空資料，結束分頁", pageNumber);
                break;
            }

            allResults.addAll(page);
            logger.debug("[StationFare] 第 {} 頁完成，本頁 {} 筆，累計 {} 筆", pageNumber, page.size(), allResults.size());

            if (page.size() < pageSize) {
                logger.debug("[StationFare] 第 {} 頁筆數 ({}) < pageSize ({})，已是最後一頁", pageNumber, page.size(), pageSize);
                break;
            }

            skip += pageSize;
            logger.debug("[StationFare] 等待 {} 秒後請求第 {} 頁", PAGE_INTERVAL_MS / 1000, pageNumber + 1);
            try {
                Thread.sleep(PAGE_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                logger.warn("[StationFare] 頁間等待被中斷，提前結束，已累計 {} 筆", allResults.size());
                break;
            }
        }

        logger.debug("[StationFare] 分頁請求全部完成，共取得 {} 筆票價資料", allResults.size());
        return allResults;
    }

    // ----- DataTaipei API 請求定義 -----

    private List<StationFacilityVO> fetchAllStationFacility() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.setAll(Map.of("scope", "resourceAquire", "limit", "1000"));

        StationFacilityApiVO response = dataTaipeiApiClientConfig.sendGetRequest(
                DATA_TAIPEI_STATION_DATASET_id, StationFacilityApiVO.class, params);

        if (response == null) {
            return Collections.emptyList();
        }

        return response.getAllStation();
    }
}
