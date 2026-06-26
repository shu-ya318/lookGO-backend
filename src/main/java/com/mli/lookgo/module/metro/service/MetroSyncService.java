package com.mli.lookgo.module.metro.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mli.lookgo.common.result.ApiResult;
import com.mli.lookgo.module.metro.client.TdxApiClient;
import com.mli.lookgo.module.metro.client.TpeApiClient;
import com.mli.lookgo.module.metro.dao.MetroDao;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.entity.StationExit;
import com.mli.lookgo.module.metro.model.entity.StationFare;
import com.mli.lookgo.module.metro.model.vo.TdxLineStationVO;
import com.mli.lookgo.module.metro.model.vo.TdxLineTransferVO;
import com.mli.lookgo.module.metro.model.vo.TdxLineVO;
import com.mli.lookgo.module.metro.model.vo.TdxODFareVO;
import com.mli.lookgo.module.metro.model.vo.TdxS2STravelTimeVO;
import com.mli.lookgo.module.metro.model.vo.TdxStationVO;
import com.mli.lookgo.module.metro.model.vo.TpeStationVO;

/**
 * 處理從外部 API 同步捷運資料到資料庫的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Service
public class MetroSyncService {

    private final TdxApiClient tdxApiClient;
    private final TpeApiClient tpeApiClient;
    private final MetroDao metroDao;
    private static final Logger logger = LoggerFactory.getLogger(MetroSyncService.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param tdxApiClient
     * @param tpeApiClient
     * @param metroDao
     */
    public MetroSyncService(TdxApiClient tdxApiClient, TpeApiClient tpeApiClient, MetroDao metroDao) {
        this.tdxApiClient = tdxApiClient;
        this.tpeApiClient = tpeApiClient;
        this.metroDao = metroDao;
    }

    /**
     * 從 TDX API 取得路線資料，轉換後同步寫入資料庫。
     *
     * @return ApiResult
     */
    @Transactional
    public ApiResult syncAllLine() {
        logger.debug("開始從 TDX 同步路線資料");

        List<TdxLineVO> tdxStationVOs = tdxApiClient.getAllLine();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<Line> lines = new ArrayList<>(tdxStationVOs.size());
        for (TdxLineVO tdxLineVO : tdxStationVOs) {
            lines.add(this.toLineEntity(tdxLineVO, now));
        }

        metroDao.upsertAllLine(lines);
        logger.debug("路線資料同步完成，共 {} 筆", lines.size());

        return new ApiResult("路線資料同步成功!");
    }

    /**
     * 從 TDX API 取得車站名稱，從 TPE API 取得車站設施，合併後同步寫入資料庫。
     *
     * @return ApiResult
     */
    @Transactional
    public ApiResult syncAllStation() {
        logger.debug("開始從 TDX + TPE 同步車站資料");

        List<TdxStationVO> tdxStationVOs = tdxApiClient.getAllStation();
        List<TpeStationVO> tpeStationVOs = tpeApiClient.getAllStation();

        // 以車站中文名稱為 key，建立 TPE 設施資料的 Map
        Map<String, TpeStationVO> tpeMap = tpeStationVOs.stream()
                .collect(Collectors.toMap(TpeStationVO::getStationName, vo -> vo, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<Station> stations = new ArrayList<>(tdxStationVOs.size());
        for (TdxStationVO tdxStationVO : tdxStationVOs) {
            TpeStationVO tpeVO = tpeMap.get(tdxStationVO.getNameZhTw());
            stations.add(this.toStationEntity(tdxStationVO, tpeVO, now));
        }

        metroDao.upsertAllStation(stations);
        logger.debug("車站資料同步完成，共 {} 筆", stations.size());

        return new ApiResult("車站資料同步成功!");
    }

    /**
     * 從 TDX API 取得路線車站資料，解析後同步寫入資料庫。
     * 需先同步路線和車站資料，以便解析外鍵 (line_id, station_id)。
     *
     * @return ApiResult
     */
    @Transactional
    public ApiResult syncAllLineStation() {
        logger.debug("開始從 TDX 同步路線車站資料");

        // 從資料庫取得現有的路線和車站，建立對應 Map 以解析外鍵
        Map<String, Short> lineLetterToIdMap = metroDao.getAllLine().stream()
                .collect(Collectors.toMap(Line::getLetter, Line::getId, (a, b) -> a));

        Map<String, Integer> stationNameToIdMap = metroDao.getAllStation().stream()
                .collect(Collectors.toMap(Station::getNameZhTw, Station::getId, (a, b) -> a));

        List<TdxLineStationVO> tdxLineStationVOs = tdxApiClient.getAllLineStation();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<LineStation> lineStations = new ArrayList<>();
        for (TdxLineStationVO tdxLineStationVO : tdxLineStationVOs) {
            // 只取 Direction=0 (去程) 避免重複
            if (tdxLineStationVO.getDirection() != null && tdxLineStationVO.getDirection() != 0) {
                continue;
            }

            Short lineId = lineLetterToIdMap.get(tdxLineStationVO.getLineId());
            if (lineId == null) {
                logger.warn("找不到路線代號 {} 對應的資料庫 id，跳過", tdxLineStationVO.getLineId());
                continue;
            }

            for (TdxLineStationVO.StationDetail detail : tdxLineStationVO.getStations()) {
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

        metroDao.upsertAllLineStation(lineStations);
        logger.debug("路線車站資料同步完成，共 {} 筆", lineStations.size());

        return new ApiResult("路線車站資料同步成功!");
    }

    /**
     * 從 TPE API 取得車站設施資料（含電梯、電扶梯），解析後同步寫入資料庫。
     * 需先同步車站資料，以便解析外鍵 (station_id)。
     *
     * @return ApiResult
     */
    @Transactional
    public ApiResult syncAllStationExit() {
        logger.debug("開始從 TPE 同步車站出口資料");

        Map<String, Integer> stationNameToIdMap = metroDao.getAllStation().stream()
                .collect(Collectors.toMap(Station::getNameZhTw, Station::getId, (a, b) -> a));

        List<TpeStationVO> tpeStationVOs = tpeApiClient.getAllStation();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<StationExit> stationExits = new ArrayList<>(tpeStationVOs.size());
        for (TpeStationVO tpeVO : tpeStationVOs) {
            Integer stationId = stationNameToIdMap.get(tpeVO.getStationName());
            if (stationId == null) {
                logger.warn("找不到車站名稱 {} 對應的資料庫 id，跳過", tpeVO.getStationName());
                continue;
            }

            stationExits.add(this.toStationExitEntity(tpeVO, stationId, now));
        }

        metroDao.upsertAllStationExit(stationExits);
        logger.debug("車站出口資料同步完成，共 {} 筆", stationExits.size());

        return new ApiResult("車站出口資料同步成功!");
    }

    /**
     * 從 TDX S2STravelTime API 取得相鄰車站行駛時間，計算累計行駛時間後更新資料庫。
     * 需先同步路線車站資料，以確保 station_code 存在。
     * 每個 LineID 只取第一筆 RouteID 避免反向重複計算。
     *
     * @return ApiResult
     */
    @Transactional
    public ApiResult syncAllLineStationCumulativeTime() {
        logger.debug("開始從 TDX S2STravelTime 同步累計行駛時間");

        Map<String, Short> lineLetterToIdMap = metroDao.getAllLine().stream()
                .collect(Collectors.toMap(Line::getLetter, Line::getId, (a, b) -> a));

        List<TdxS2STravelTimeVO> s2sTravelTimeVOs = tdxApiClient.getAllS2STravelTime();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 每個 LineID 只取第一筆（避免同一路線不同 RouteID 重複覆寫）
        Set<String> processedLineIds = new HashSet<>();
        List<LineStation> lineStations = new ArrayList<>();

        for (TdxS2STravelTimeVO vo : s2sTravelTimeVOs) {
            if (!processedLineIds.add(vo.getLineId())) {
                continue;
            }

            Short lineId = lineLetterToIdMap.get(vo.getLineId());
            if (lineId == null) {
                logger.warn("找不到路線代號 {} 對應的資料庫 id，跳過", vo.getLineId());
                continue;
            }

            List<TdxS2STravelTimeVO.TravelTimeDetail> travelTimes = vo.getTravelTimes();
            if (travelTimes == null || travelTimes.isEmpty()) {
                continue;
            }

            // 逐段累加：cumulative_time = Σ (StopTime[i] + RunTime[i])
            int cumulativeTime = 0;
            for (int i = 0; i < travelTimes.size(); i++) {
                TdxS2STravelTimeVO.TravelTimeDetail td = travelTimes.get(i);

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
            metroDao.updateAllLineStationCumulativeTime(lineStations);
        }
        logger.debug("累計行駛時間同步完成，共更新 {} 筆", lineStations.size());

        return new ApiResult("路線車站累計行駛時間同步成功!");
    }

    /**
     * 將 TDX 回傳的 TdxLineVO 轉換為資料庫的 Line 實體，補上 status 和 updatedAt。
     *
     * @param vo  TDX 回傳的路線資料
     * @param now 當下 UTC 時間
     * @return Line
     */
    private Line toLineEntity(TdxLineVO tdxLineVO, LocalDateTime updatedAt) {
        return new Line(
                tdxLineVO.getLetter(),
                tdxLineVO.getNameZhTw(),
                tdxLineVO.getNameEn(),
                tdxLineVO.getColor(),
                1, // 考量: 預設有資料即為啟用狀態
                updatedAt);
    }

    /**
     * 合併 TDX 車站名稱和 TPE 車站設施資料，轉換為資料庫的 Station 實體。
     *
     * @param tdxStationVO TDX 回傳的車站名稱資料
     * @param tpeVO        TPE 回傳的車站設施資料（可能為 null，表示該站無對應設施資料）
     * @param updatedAt    當下 UTC 時間
     * @return Station
     */
    private Station toStationEntity(TdxStationVO tdxStationVO, TpeStationVO tpeVO,
            LocalDateTime updatedAt) {
        return new Station(
                tdxStationVO.getNameZhTw(),
                tdxStationVO.getNameEn(),
                1, // 考量: 預設有資料即為啟用狀態
                tpeVO != null ? tpeVO.getAtm() : null,
                tpeVO != null ? tpeVO.getNursingRoom() : null,
                tpeVO != null ? tpeVO.getDiaperTable() : null,
                tpeVO != null ? tpeVO.getChargingStation() : null,
                tpeVO != null ? tpeVO.getTicketMachine() : null,
                tpeVO != null ? tpeVO.getDrinkingWater() : null,
                tpeVO != null ? tpeVO.getRestroom() : null,
                updatedAt);
    }

    /**
     * 從 TDX ODFare API 取得任意兩站間的票價，解析後同步寫入資料庫。
     * 需先同步路線車站資料，以確保 station_code 存在。
     * CitizenCode 城市優惠票 (FareClass=3) 因資料表無對應欄位，同步時跳過。
     *
     * @return ApiResult
     */
    @Transactional
    public ApiResult syncAllStationFare() {
        logger.debug("開始從 TDX ODFare 同步票價資料");

        // 從 lines_stations 建立 station_code → station_id 對應 Map
        Map<String, Integer> stationCodeToIdMap = metroDao.getAllLineStation().stream()
                .filter(ls -> ls.getStationCode() != null && ls.getStationId() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, LineStation::getStationId, (a, b) -> a));

        List<TdxODFareVO> odFareVOs = tdxApiClient.getAllODFare();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<StationFare> stationFares = new ArrayList<>();
        for (TdxODFareVO odFare : odFareVOs) {
            Integer fromStationId = stationCodeToIdMap.get(odFare.getOriginStationId());
            Integer toStationId = stationCodeToIdMap.get(odFare.getDestinationStationId());

            if (fromStationId == null || toStationId == null) {
                logger.warn("找不到站點 {} 或 {} 對應的資料表 id，跳過",
                        odFare.getOriginStationId(), odFare.getDestinationStationId());
                continue;
            }

            for (TdxODFareVO.FareDetail fareDetail : odFare.getFares()) {
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
            metroDao.upsertAllStationFare(batch);
            totalInserted += batch.size();
            logger.debug("票價資料批次寫入進度：{} / {} 筆", totalInserted, stationFares.size());
        }

        logger.debug("票價資料同步完成，共 {} 筆", stationFares.size());

        return new ApiResult("票價資料同步成功!");
    }

    /**
     * 從 TDX LineTransfer API 取得路線換乘資料，解析後同步寫入資料庫。
     * 需先同步路線車站資料，以確保 station_code → lines_stations.id 對應存在。
     *
     * @return ApiResult
     */
    @Transactional
    public ApiResult syncAllLineTransfer() {
        logger.debug("開始從 TDX LineTransfer 同步路線換乘資料");

        // 從 lines_stations 建立 station_code → lines_stations.id 對應 Map
        Map<String, Integer> stationCodeToLineStationIdMap = metroDao.getAllLineStation().stream()
                .filter(ls -> ls.getStationCode() != null && ls.getId() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, LineStation::getId, (a, b) -> a));

        List<TdxLineTransferVO> lineTransferVOs = tdxApiClient.getAllLineTransfer();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<LineTransfer> lineTransfers = new ArrayList<>();
        for (TdxLineTransferVO vo : lineTransferVOs) {
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
            metroDao.upsertAllLineTransfer(lineTransfers);
        }
        logger.debug("路線換乘資料同步完成，共 {} 筆", lineTransfers.size());

        return new ApiResult("路線換乘資料同步成功!");
    }

    /**
     * 將 TPE 回傳的 TpeStationVO 中的電梯電扶梯欄位轉換為資料庫的 StationExit 實體。
     *
     * @param tpeVO     TPE 回傳的車站設施資料
     * @param stationId 已解析的資料庫車站 ID
     * @param updatedAt 當下 UTC 時間
     * @return StationExit
     */
    private StationExit toStationExitEntity(TpeStationVO tpeVO, Integer stationId,
            LocalDateTime updatedAt) {
        return new StationExit(
                stationId,
                tpeVO.getElevator(),
                tpeVO.getEscalator(),
                updatedAt);
    }
}
