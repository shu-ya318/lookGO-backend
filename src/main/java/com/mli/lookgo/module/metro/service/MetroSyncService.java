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

    // 新北投 (R22A)、小碧潭 (G03A) 為獨立接駁區間車站，幹線列車不會直達，乘客必須下車走月台換乘，
    // 與蘆洲 (O54)、迴龍 (O21) 屬幹線列車直達分岔不同（見 MetroForkBranchRouteGraphService）。
    // TDX S2STravelTime 僅提供純行駛與停靠秒數，未涵蓋此月台轉乘時間，故在此手動加上估計秒數，
    // 使其併入該站的 cumulative_time，讓下游計算（無論同線任何站對）都用同一套累計時間相減公式，不需另外特例處理。
    // 兩站月台配置與走行距離不同，轉乘秒數各自依官方公布的門到門總時間反推，不可共用同一數值：
    // 新北投依官方標示步行 3 分鐘；小碧潭依官方 4 分鐘總時間扣除純車程 203 秒反推約 37 秒
    private static final Map<String, Integer> SHUTTLE_TRANSFER_SECONDS_BY_CODE = Map.of(
            "R22A", 180,
            "G03A", 37);

    // DataTaipei 車站設施資料集為與台鐵、高鐵同名的板橋站區隔，將捷運板橋站命名為「板橋(板南線)」，
    // 與 TDX 車站中文名稱「板橋」不一致，故在合併資料前正規化為 TDX 命名，以此 Map 為唯一真實來源
    private static final Map<String, String> DATA_TAIPEI_STATION_NAME_ALIASES = Map.of(
            "板橋(板南線)", "板橋");

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
        List<StationFacilityVO> dataTaipeiStationVOs = fetchAllStationFacility();

        // 以車站中文名稱為 key，建立 DataTaipei 設施資料的 Map，方便查詢
        // 名稱先經過 normalizeDataTaipeiStationName 正規化，修正與 TDX 命名不一致的已知特例（如板橋站）
        Map<String, StationFacilityVO> dataTaipeiMap = dataTaipeiStationVOs.stream()
                .collect(Collectors.toMap(vo -> normalizeDataTaipeiStationName(vo.getStationName()), vo -> vo,
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

        // 2. 取得資料庫所有車站，建立「原始車站中文名稱 -> 車站 id」的對照 Map。例如:{"淡水" -> 101, "紅樹林" -> 102}
        // 須以 originalNameZhTw（同步比對鍵）而非 nameZhTw（管理端可能已修改的顯示名稱）比對，
        // 避免管理員改名後，此處依 TDX 原始站名比對不到而遺漏該站的路線關聯
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
     * 同一 LineId 可能對應多筆路線資料：同路線的去回程重複資料、像中和新蘆線這種 Y
     * 字分岔路線蘆洲／迴龍兩條支線共用同一個 LineId 的資料，或淡水信義線／松山新店線這種
     * 單站分支接駁區間（如北投-新北投）。故以路線實際途經的車站代碼集合（而非僅 LineId）判斷是否為
     * 重複路線，避免分岔支線被誤判為重複而整條略過；並依途經站數由多到少處理，確保幹線（站數最多）
     * 優先確立每站的累計時間基準，較短的分支接駁區間只能接續基準值繼續累加未確立的新站，
     * 不會覆寫幹線站已確立的值（見 {@link #addLineStationCumulativeTimeIfAbsent}）。
     * 較短的分支接駁區間中，TDX 回傳的車站順序不一定是「幹線交會站在前」（例如蘆洲支線資料是從
     * 蘆洲往大橋頭方向排列，大橋頭在最後一站才出現）。故不直接依「起點站是否已確立」決定起算值，
     * 而是先以路線資料自身原始順序、從 0 秒起算，得出這筆資料內部一致的相對數值（同一實體區間在不同
     * 路線資料中的秒數彼此相符，只是各自起點不同），再找出資料中任一已確立基準的車站，算出平移量套用到
     * 整筆資料，將其平移至與幹線相同的基準（見 {@link #shiftToEstablishedBaseline}）。
     *
     * @return MessageVO
     */
    @Transactional
    public MessageVO syncAllLineStationCumulativeTime() {
        logger.debug("開始從 TDX StationTravelTime API 同步累計行駛時間");

        // 1. 取得資料庫所有路線，建立「路線代號 -> 路線 id」的對照 Map。例如:{"R" -> 2}
        Map<String, Short> lineLetterToIdMap = metroDAO.getAllLine().stream()
                .collect(Collectors.toMap(Line::getLetter, Line::getId, (existingValue, newValue) -> existingValue));

        // 2. 從 TDX API 取得所有車站間的行駛與停站時間資料，依途經站數由多到少排序，
        // 讓幹線（或像橘線兩條完整支線）優先於較短的分支接駁區間被處理，確立全域基準
        List<StationTravelTimeVO> s2sTravelTimeVOs = fetchAllStationTravelTime().stream()
                .filter(vo -> vo.getTravelTimes() != null && !vo.getTravelTimes().isEmpty())
                .sorted(Comparator.comparingInt(
                        (StationTravelTimeVO vo) -> vo.getTravelTimes().size()).reversed())
                .collect(Collectors.toList());
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

        // 3. 用於追蹤已處理的路線（以途經車站集合為鍵），避免同路線去回程重複覆寫
        Set<String> processedRouteKeys = new HashSet<>();
        // 4. 追蹤已確立累計時間基準的車站代碼，車站代碼全域唯一，故不需按路線區分
        Map<String, Integer> establishedCumulativeTimeByCode = new HashMap<>();
        List<LineStation> lineStations = new ArrayList<>();

        for (StationTravelTimeVO vo : s2sTravelTimeVOs) {
            List<StationTravelTimeVO.TravelTimeDetail> travelTimes = vo.getTravelTimes();

            // 5. 以「LineId + 途經車站代碼集合（排序後去向無關）」建立路線識別鍵，
            // 去回程資料途經車站相同會得到同一把鍵而被略過；分岔支線因途經車站不同（如蘆洲支線含
            // O50~O54、迴龍支線含 O13~O21）會得到不同的鍵，兩條支線才都會被處理到
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

            // 7. 依路線資料原始順序，從 0 秒起算，得出各站相對於此路線資料自身起點的累計秒數，
            // 逐站累加停站、行駛秒數（例如：累加淡水停站 30 秒 + 淡水到紅樹林行車 130 秒，累加後為 160 秒），
            // 若該站為獨立接駁分支站（新北投、小碧潭），額外加上月台轉乘秒數
            List<String> stationCodesInOrder = new ArrayList<>();
            stationCodesInOrder.add(travelTimes.get(0).getFromStationId());
            List<Integer> localCumulativeTimes = new ArrayList<>();
            localCumulativeTimes.add(0);
            for (StationTravelTimeVO.TravelTimeDetail td : travelTimes) {
                stationCodesInOrder.add(td.getToStationId());
                int nextTime = localCumulativeTimes.get(localCumulativeTimes.size() - 1)
                        + td.getStopTime() + td.getRunTime();
                // 起訖任一端為接駁分支站即視為跨越該轉乘區間，不論 TDX 回傳方向為何，
                // 轉乘秒數依該分支站各自的估計值計算，而非共用同一數值
                Integer shuttleTransferSeconds = SHUTTLE_TRANSFER_SECONDS_BY_CODE.getOrDefault(
                        td.getToStationId(), SHUTTLE_TRANSFER_SECONDS_BY_CODE.get(td.getFromStationId()));
                if (shuttleTransferSeconds != null) {
                    nextTime += shuttleTransferSeconds;
                }
                localCumulativeTimes.add(nextTime);
            }

            // 8. 將此路線資料自身的相對數值，平移至與已確立車站相同的全域基準後，逐站寫入
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
     * 找出路線資料中任一已確立累計時間基準的車站，算出「該站已確立值」與「該站此路線資料自身相對值」的
     * 差，作為平移量：由於同一實體區間在不同路線資料中的秒數彼此相符，只是各自路線起點不同，
     * 平移後即可讓整筆資料與幹線基準一致，不論該已確立車站落在資料中的第一站、最後一站或中間站皆適用。
     * 若整筆資料都尚未有任何車站確立基準（代表這是目前途經站數最多、最先處理的路線），平移量為 0，
     * 直接以自身數值作為全域基準。
     *
     * @param stationCodesInOrder             依路線資料原始順序排列的車站代碼
     * @param localCumulativeTimes            對應各車站、僅相對於此路線資料自身起點的累計秒數
     * @param establishedCumulativeTimeByCode 已確立累計時間的車站代碼對照表
     * @return 平移量（秒），套用於 localCumulativeTimes 可得到與全域基準一致的數值
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
        return 0;
    }

    /**
     * 建立車站累計時間記錄並加入待寫入清單；若該車站代碼已由先前處理、途經站數較多的路線確立累計時間，
     * 則略過不重複寫入，確保幹線的基準值不會被之後處理、途經站數較少的分支接駁路線（如北投-新北投）覆寫。
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
     * 將 DataTaipei 車站設施資料的中文名稱正規化為 TDX 命名，修正已知的資料源命名不一致特例
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
