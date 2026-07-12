package com.mli.lookgo.module.metro.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import com.mli.lookgo.module.metro.exceptions.SyncInProgressException;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.vo.LineStationVO;
import com.mli.lookgo.module.metro.model.vo.LineTransferVO;
import com.mli.lookgo.module.metro.model.vo.LineVO;
import com.mli.lookgo.module.metro.model.vo.StationFacilityApiVO;
import com.mli.lookgo.module.metro.model.vo.StationFacilityVO;
import com.mli.lookgo.module.metro.model.vo.StationTravelTimeVO;
import com.mli.lookgo.module.metro.model.vo.StationVO;
import com.mli.lookgo.module.metro.model.vo.SyncStatusVO;

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

    private final StationFareSyncStateHolder stationFareSyncStateHolder;
    private final StationFareSyncWorker stationFareSyncWorker;

    private static final Logger logger = LoggerFactory.getLogger(MetroSyncService.class);

    // 資料來源 DataTaipei: 請求車站設施的資料集 id
    private static final String DATA_TAIPEI_STATION_DATASET_id = "f69dfd66-3d8e-408a-9645-c02384bda5b8";

    /*
     * 獨立區間車站 (包含新北投 、小碧潭) 必須下車走月台轉乘。
     * 但目前 TDX S2STravelTime 無包含月台轉乘時間，依照北捷官方資料在 cumulative_time 加入轉乘秒數。
     */
    private static final Map<String, Integer> SHUTTLE_TRANSFER_SECONDS_BY_CODE = Map.of(
            "R22A", 180,
            "G03A", 37);

    // 處理例外情形: 比對車站中文名稱寫入 stations 表時，資料來源 DataTaipei 與 TDX 對於板橋命名不一致
    private static final Map<String, String> DATA_TAIPEI_STATION_NAME_ALIASES = Map.of(
            "板橋(板南線)", "板橋");

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param tdxApiClientConfig
     * @param dataTaipeiApiClientConfig
     * @param metroDAO
     * @param stationFareSyncStateHolder
     * @param stationFareSyncWorker
     */
    public MetroSyncService(TDXApiClientConfig tdxApiClientConfig, DataTaipeiApiClientConfig dataTaipeiApiClientConfig,
            MetroDAO metroDAO, StationFareSyncStateHolder stationFareSyncStateHolder,
            StationFareSyncWorker stationFareSyncWorker) {
        this.tdxApiClientConfig = tdxApiClientConfig;
        this.dataTaipeiApiClientConfig = dataTaipeiApiClientConfig;
        this.metroDAO = metroDAO;
        this.stationFareSyncStateHolder = stationFareSyncStateHolder;
        this.stationFareSyncWorker = stationFareSyncWorker;
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
     * 從 TDX API 取得車站名稱，再從 DataTaipei API 取得對應的車站設施，合併後同步寫入資料庫。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllStation() {
        logger.debug("開始從 TDX + DataTaipei 同步請求車站資料");

        List<StationVO> tdxStationVOs = fetchAllStation();
        List<StationFacilityVO> dataTaipeiStationVOs = fetchAllStationFacility();

        /*
         * 資料來源 DataTaipei : 以車站中文名稱為 key，建立車站設施資料的 Map，方便後續查詢。
         * 且名稱先經過正規化，修正與 TDX 命名不一致的特例（如板橋站）
         */
        Map<String, StationFacilityVO> dataTaipeiMap = dataTaipeiStationVOs.stream()
                .collect(Collectors.toMap(vo -> normalizeDataTaipeiStationName(vo.getStationName()), vo -> vo,
                        // 車站不得重複: 若有相同 key，保留第一次出現的值
                        (existingValue, newValue) -> existingValue));

        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        /*
         * 透過車站名稱比對來合併不同來源資料。
         * 並處理轉乘站會有多筆重複站名情形，進行去重只儲存第一筆
         */
        Map<String, Station> stationByName = new HashMap<>();

        for (StationVO tdxStationVO : tdxStationVOs) {
            StationFacilityVO dataTaipeiVO = dataTaipeiMap.get(tdxStationVO.getNameZhTw());
            stationByName.putIfAbsent(tdxStationVO.getNameZhTw(),
                    this.toStationEntity(tdxStationVO, dataTaipeiVO, currentTime));
        }
        List<Station> stations = new ArrayList<>(stationByName.values());

        // 設定 batchSize = 150 分批 Upsert: 避免每筆有 13 個參數，總參數數易超過 SQL Server 單次請求上限( 2100 )
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
     * 0. 需先同步路線和車站資料，以便取得外鍵 (line_id, station_id)。
     * 1. 從 TDX API 取得路線車站資料，再同步寫入資料庫。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLineStation() {
        logger.debug("開始從 TDX 同步路線車站資料");

        // 1. 取得資料庫所有路線，建立「路線代號 -> 路線 id」的對照 Map。例如:{"R" -> 2}
        Map<String, Short> lineLetterToIdMap = metroDAO.getAllLine().stream()
                .collect(Collectors.toMap(Line::getLetter, Line::getId, (existingValue, newValue) -> existingValue));

        // 2. 取得資料庫所有車站，建立「原始車站中文名稱 -> 車站 id」的對照 Map。例如:{"淡水" -> 101, "紅樹林" -> 102}
        // 比對方式: 以 originalNameZhTw（同步比對鍵）而非 nameZhTw（管理端可能已修改的顯示名稱），
        // 避免管理員改名後，依 TDX 原始站名比對失敗而遺漏該站的路線關聯。
        Map<String, Integer> stationNameToIdMap = metroDAO.getAllStation().stream()
                .collect(Collectors.toMap(Station::getOriginalNameZhTw, Station::getId,
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
     * 0. 需先同步路線車站資料，確保 station_code 存在。
     * 1. 先計算同路線每個車站的相對時間: 從第一站歸零起算，逐站累加秒數。
     * 2-1. 如果路線間有共用車站，則計算平移量並累加到經過車站：
     * (1). 平移量 = 全域基準時間(共用車站) - 這筆資料自己算出的相對時間(共用車站)。
     * (2). 每站真正時間 = 這站的相對時間 + 平移量
     * 2-2. 不依照車站代號順序相加，避免分叉路線、分支車站會計算錯誤。
     * 以橘線為例，先算蘆洲或迴龍分支各站時間，再以共用的大橋頭站為基準，計算累積時間加上大橋頭站的累積秒數。
     * 
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLineStationCumulativeTime() {
        logger.debug("開始從 TDX StationTravelTime API 同步累計行駛時間");

        // 1. 取得資料庫所有路線，建立「路線代號 -> 路線 id」的對照 Map。例如:{"R" -> 2}
        Map<String, Short> lineLetterToIdMap = metroDAO.getAllLine().stream()
                .collect(Collectors.toMap(Line::getLetter, Line::getId, (existingValue, newValue) -> existingValue));

        // 2. 依路線長度（站數）排序優先處理:
        // 從 TDX API 取得所有車站間的行駛與停站時間資料，依途經站數由多到少排序，
        // 讓主幹線或長路線優先被處理，確定全域基準時間
        List<StationTravelTimeVO> s2sTravelTimeVOs = fetchAllStationTravelTime().stream()
                .filter(vo -> vo.getTravelTimes() != null && !vo.getTravelTimes().isEmpty())
                .sorted(Comparator.comparingInt(
                        (StationTravelTimeVO vo) -> vo.getTravelTimes().size()).reversed())
                .collect(Collectors.toList());
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        // 3. 追蹤已處理的路線（以途經車站集合為鍵）
        Set<String> processedRouteKeys = new HashSet<>();
        // 4. 追蹤已建立全域基準時間的車站代碼
        Map<String, Integer> establishedCumulativeTimeByCode = new HashMap<>();
        List<LineStation> lineStations = new ArrayList<>();

        for (StationTravelTimeVO vo : s2sTravelTimeVOs) {
            List<StationTravelTimeVO.TravelTimeDetail> travelTimes = vo.getTravelTimes();

            // 5. 去回程去重與分叉路線判定 ， 以「LineId + 途經車站代碼集合（排序後去向無關）」建立路線識別的鍵，
            // (1). 去回程視為相同 (因途經車站相同得到同鍵，被略過)
            // (2). 分岔路線視為不同 (因途經車站不同得到不同鍵，確保各支線被獨立處理)
            Set<String> stationCodesInRoute = travelTimes.stream()
                    .flatMap(detail -> Stream.of(detail.getFromStationId(), detail.getToStationId()))
                    .collect(Collectors.toCollection(TreeSet::new));
            String routeKey = vo.getLineId() + ":" + String.join(",", stationCodesInRoute);

            if (!processedRouteKeys.add(routeKey)) {
                continue;
            }

            // 6. 依路線代號查出資料庫對應的 lineId，找不到則警告並跳過
            Short lineId = lineLetterToIdMap.get(vo.getLineId());
            if (lineId == null) {
                logger.warn("找不到路線代號 {} 對應的資料庫 id，跳過", vo.getLineId());
                continue;
            }

            // 7. 計算局部相對時間: 
            // (1) 依路線資料原始順序，起點站以 0 秒起算
            // (2) 逐站累加「前站的停站秒數 + 兩站間行駛秒數」
            // (3) 獨立分支站（新北投、小碧潭）特殊處理: 額外加上月台轉乘秒數
            List<String> stationCodesInOrder = new ArrayList<>();
            stationCodesInOrder.add(travelTimes.get(0).getFromStationId());
            List<Integer> localCumulativeTimes = new ArrayList<>();
            localCumulativeTimes.add(0);
            for (StationTravelTimeVO.TravelTimeDetail td : travelTimes) {
                stationCodesInOrder.add(td.getToStationId());
                int nextTime = localCumulativeTimes.get(localCumulativeTimes.size() - 1)
                        + td.getStopTime() + td.getRunTime();
                // 起訖任一端為接駁分支站即視為跨越該轉乘區間，不論 TDX 回傳方向為何，
                // 轉乘秒數: 依該分支站各自的估計值計算，而非共用同一數值
                Integer shuttleTransferSeconds = SHUTTLE_TRANSFER_SECONDS_BY_CODE.getOrDefault(
                        td.getToStationId(), SHUTTLE_TRANSFER_SECONDS_BY_CODE.get(td.getFromStationId()));
                if (shuttleTransferSeconds != null) {
                    nextTime += shuttleTransferSeconds;
                }
                localCumulativeTimes.add(nextTime);
            }

            // 8. 對齊全域基準:
            // 將此路線資料自身的相對數值，平移至與已確立車站相同的全域基準後，逐站寫入
            // （已確立基準的車站會在 addLineStationCumulativeTimeIfAbsent 內被略過，不會被覆寫）
            int offset = shiftToEstablishedBaseline(stationCodesInOrder, localCumulativeTimes,
                    establishedCumulativeTimeByCode);

            for (int i = 0; i < stationCodesInOrder.size(); i++) {
                addLineStationCumulativeTimeIfAbsent(lineStations, establishedCumulativeTimeByCode,
                        lineId, stationCodesInOrder.get(i), localCumulativeTimes.get(i) + offset, currentTime);
            }
        }

        // 9. 若有成功解析出累計時間資料，則整批更新寫入資料庫
        if (!lineStations.isEmpty()) {
            metroDAO.updateAllLineStationCumulativeTime(lineStations);
        }
        logger.debug("累計行駛時間同步完成，共更新 {} 筆", lineStations.size());

        return new MessageVO("路線車站累計行駛時間同步成功!");
    }

    /**
     * 計算平移量。
     * 例如:大橋頭在全域基準的時間：1200 秒，在蘆洲分支算出的累計時間為 750 秒，平移量為兩者相減 (450 秒)。
     * 
     * @param stationCodesInOrder             依路線資料原始順序排列的車站代碼
     * @param localCumulativeTimes            對應各車站、僅相對於此路線資料自身起點的累計秒數
     * @param establishedCumulativeTimeByCode 已確立累計時間的車站代碼對照表
     * @return 平移量（秒），套用於 localCumulativeTimes 得到與全域基準一致的數值
     */
    private int shiftToEstablishedBaseline(
            List<String> stationCodesInOrder,
            List<Integer> localCumulativeTimes,
            Map<String, Integer> establishedCumulativeTimeByCode) {

        for (int i = 0; i < stationCodesInOrder.size(); i++) {
            Integer established = establishedCumulativeTimeByCode.get(stationCodesInOrder.get(i));

            if (established != null) {
                return established - localCumulativeTimes.get(i);
            }
        }

        // 處理第一個計算的路線沒有基準車站情形。
        return 0;
    }

    /**
     * 1. 紀錄車站的累計時間，加入待寫入清單。
     * 2. 若車站代碼已由先前處理、途經站數較多的路線確立累計時間，則略過，
     * 確保幹線的基準值不被之後處理、途經站數較少的分支接駁路線覆寫。
     * 例如: 1. 分岔路線的共用車站 (大橋頭): 已被站數多的迴龍計算，蘆洲分支就不再計算
     * 2. 獨立分支車站 (新北投、小碧潭): 隸屬於獨立分支，仍正常寫入。
     * 
     * @param lineStations                    待寫入資料庫的路線車站清單，符合條件時會加入新記錄
     * @param establishedCumulativeTimeByCode 已確立累計時間的車站代碼對照表，會加入新確立的車站
     * @param lineId                          路線 id
     * @param stationCode                     車站代碼
     * @param cumulativeTime                  該車站的累計行駛秒數
     * @param updatedAt                       更新時間 (UTC)
     */
    private void addLineStationCumulativeTimeIfAbsent(
            List<LineStation> lineStations,
            Map<String, Integer> establishedCumulativeTimeByCode,
            Short lineId,
            String stationCode,
            int cumulativeTime,
            LocalDateTime updatedAt) {

        if (establishedCumulativeTimeByCode.containsKey(stationCode)) {
            return;
        }

        establishedCumulativeTimeByCode.put(stationCode, cumulativeTime);

        LineStation lineStation = new LineStation();

        lineStation.setLineId(lineId);
        lineStation.setStationCode(stationCode);
        lineStation.setCumulativeTime((short) cumulativeTime);
        lineStation.setUpdatedAt(updatedAt);

        lineStations.add(lineStation);
    }

    /**
     * 觸發背景同步票價資料。
     * 透過 {@link StationFareSyncStateHolder#tryStart()} 確保同時只有一個同步在執行，
     * 若已有同步進行中則拋出 {@link SyncInProgressException}；否則交由 {@link StationFareSyncWorker} 於獨立執行緒非同步執行，方法本身立即返回。
     * 手動觸發與排程共用的起始點，確保兩者不會並發執行同步。
     *
     * @throws SyncInProgressException 已有票價同步進行中時拋出（由 GlobalExceptionHandler 轉為 409）
     */
    public void startSyncAllStationFare() {
        if (!stationFareSyncStateHolder.tryStart()) {
            throw new SyncInProgressException("票價同步正在進行中，請勿重複觸發!");
        }
        
        logger.debug("已取得票價同步啟動權，交由背景執行緒執行");
        stationFareSyncWorker.doSyncAllStationFare();
    }

    /**
     * 取得票價背景同步的當前狀態快照。
     *
     * @return SyncStatusVO
     */
    public SyncStatusVO getStationFareSyncStatus() {
        return stationFareSyncStateHolder.snapshot();
    }

    /**
     * 0. 需先同步路線車站資料，以確保 station_code → lines_stations.id 對應存在。
     * 1. 從 TDX LineTransfer API 取得路線轉乘資料，同步寫入資料庫。
     * 
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLineTransfer() {
        logger.debug("開始從 TDX LineTransfer 同步路線轉乘資料");

        // 1. 取得資料庫所有路線車站關聯，建立「車站代碼 -> 路線車站關聯表 id」的對照 Map。例如:{"BL12" -> 30, "R10" -> 12}
        Map<String, Integer> stationCodeToLineStationIdMap = metroDAO.getAllLineStation().stream()
                .filter(ls -> ls.getStationCode() != null && ls.getId() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, LineStation::getId,
                        (existingValue, newValue) -> existingValue));

        // 2. 從 TDX API 取得所有轉乘站的路線間轉乘（步行）時間資料
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

        final int batchSize = 500;
        int totalUpserted = 0;
        for (int i = 0; i < lineTransfers.size(); i += batchSize) {
            List<LineTransfer> batch = lineTransfers.subList(i, Math.min(i + batchSize, lineTransfers.size()));
            metroDAO.upsertAllLineTransfer(batch);
            totalUpserted += batch.size();
            logger.debug("路線轉乘資料批次寫入進度：{} / {} 筆", totalUpserted, lineTransfers.size());
        }
        logger.debug("路線轉乘資料同步完成，共 {} 筆", lineTransfers.size());

        return new MessageVO("路線轉乘資料同步成功!");
    }

    /**
     * 檢查資料庫中是否已有捷運路線資料，用於判斷是否首次部署或容器初始化作為排程啟動依據。
     *
     * @return 若路線資料表為空則回傳 true
     */
    public boolean isMetroDataEmpty() {
        return metroDAO.getAllLine().isEmpty();
    }

    // ----- private helper -----

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
     * 將 DataTaipei 車站設施資料的中文名稱正規化為 TDX 命名，修正已知的資料源命名不一致特例。
     * （見 {@link #DATA_TAIPEI_STATION_NAME_ALIASES}）。
     *
     * @param dataTaipeiStationName DataTaipei 回傳的原始車站中文名稱
     * @return 正規化後、與 TDX 命名一致的車站中文名稱
     */
    private String normalizeDataTaipeiStationName(String dataTaipeiStationName) {
        return DATA_TAIPEI_STATION_NAME_ALIASES.getOrDefault(dataTaipeiStationName, dataTaipeiStationName);
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
