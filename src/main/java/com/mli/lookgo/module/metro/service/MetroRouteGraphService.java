package com.mli.lookgo.module.metro.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mli.lookgo.module.metro.enums.StationFacilities;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.graph.DijkstraResult;
import com.mli.lookgo.module.metro.model.graph.Edge;
import com.mli.lookgo.module.metro.model.vo.OriginDestinationDetailVO;

/**
 * 處理捷運路線的業務邏輯。
 *
 * @author D5042101
 * @since 2026.07.02
 */
@Service
public class MetroRouteGraphService {

    private final MetroForkBranchRouteGraphService metroForkBranchRouteGraphService;

    // 起訖同站（進出同一車站）的手續費：全票 20 元，其餘優惠票種（學生、兒童、愛心）一律 8 元
    public static final BigDecimal SAME_STATION_FULL_FARE = BigDecimal.valueOf(20);
    public static final BigDecimal SAME_STATION_CONCESSION_FARE = BigDecimal.valueOf(8);

    // 全票票種代碼，用於區分同站手續費適用的費率
    private static final int FULL_FARE_TYPE = 1;

    // 策略 1（最少轉乘）轉乘邊權重的主要成分，遠大於任何實際車程秒數，確保轉乘次數為主要比較依據，
    // 實際秒數只在轉乘次數相同時作為次要比較依據
    private static final int TRANSFER_COUNT_DOMINANT_WEIGHT = 1_000_000;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param metroForkBranchRouteGraphService
     */
    public MetroRouteGraphService(MetroForkBranchRouteGraphService metroForkBranchRouteGraphService) {
        this.metroForkBranchRouteGraphService = metroForkBranchRouteGraphService;
    }

    /**
     * 起終點相同時，直接組成只含單一車站的回傳結果。
     * farePrice 依票種帶入同站手續費：全票為 {@link #SAME_STATION_FULL_FARE}，
     * 其餘優惠票種為 {@link #SAME_STATION_CONCESSION_FARE}；未傳入 fareType 時不計算票價。
     */
    public OriginDestinationDetailVO buildSameStationResult(
            String stationCode,
            Integer fareType,
            int strategy,
            Map<String, LineStation> lineStationByCode,
            Map<Short, Line> lineById,
            Map<Integer, Station> stationById,
            List<StationFacilities> stationFacilities) {

        LineStation lineStation = lineStationByCode.get(stationCode);
        Station station = stationById.get(lineStation.getStationId());
        Line line = lineById.get(lineStation.getLineId());

        OriginDestinationDetailVO.StationInfoVO stationInfo = new OriginDestinationDetailVO.StationInfoVO(
                stationCode,
                station != null ? station.getNameZhTw() : null,
                station != null ? station.getNameEn() : null);
        applyFacilities(stationInfo, station, stationFacilities);
        OriginDestinationDetailVO.RouteSegmentVO segment = new OriginDestinationDetailVO.RouteSegmentVO(
                line != null ? line.getLetter() : null,
                line != null ? line.getNameZhTw() : null,
                line != null ? line.getColor() : null,
                Collections.singletonList(stationInfo), 0);

        BigDecimal farePrice = resolveSameStationFare(fareType);

        return new OriginDestinationDetailVO(stationCode, stationCode, fareType, strategy,
                Collections.singletonList(segment), 0, 0, 0, farePrice);
    }

    /**
     * 依票種取得起訖同站的手續費。
     *
     * @param fareType 票種 (1=全票, 4=學生, 5=兒童, 7=愛心)，為 null 時代表不計算票價
     * @return 全票為 20 元，其餘優惠票種為 8 元；fareType 為 null 時回傳 null
     */
    private BigDecimal resolveSameStationFare(Integer fareType) {
        if (fareType == null) {
            return null;
        }

        return fareType == FULL_FARE_TYPE ? SAME_STATION_FULL_FARE : SAME_STATION_CONCESSION_FARE;
    }

    /**
     * 建立轉乘時間查找表，key 格式為 "fromCode:toCode"，value 為轉乘時間（分鐘）。
     * 雙向皆建立，確保正反向路線皆可查詢。
     */
    public Map<String, Short> buildTransferTimeMap(
            List<LineTransfer> lineTransfers,
            Map<Integer, LineStation> lineStationById) {

        Map<String, Short> transferTimeMap = new HashMap<>();
        for (LineTransfer lineTransfer : lineTransfers) {
            LineStation from = lineStationById.get(lineTransfer.getFromLineStationId());
            LineStation to = lineStationById.get(lineTransfer.getToLineStationId());
            if (from == null || to == null)
                continue;
            Short transferTime = lineTransfer.getTransferTime();
            transferTimeMap.put(from.getStationCode() + ":" + to.getStationCode(), transferTime);
            transferTimeMap.put(to.getStationCode() + ":" + from.getStationCode(), transferTime);
        }
        return transferTimeMap;
    }

    /**
     * 依路線策略建立鄰接表。
     * 策略 1：同線邊權重為相鄰站累計時間秒數差、轉乘邊權重為一個遠大於任何實際秒數的常數
     * 加上轉乘時間秒數（最小化轉乘次數；轉乘次數相同時，以實際秒數作為次要比較依據，
     * 避免任意選到轉乘次數相同但多繞路的路徑）。
     * 
     * 策略 2：同線邊權重為相鄰站累計時間秒數差、轉乘邊權重為轉乘時間秒數（最短車程時間）。
     * 具 Y 字分岔的路線（見 {@link MetroForkBranchRouteGraphService}）
     * 無法單純依 stationSequence排序推導同線邊，分岔口相鄰站對會略過線性推導，改由
     * {@link MetroForkBranchRouteGraphService#addBranchEdges} 建立正確邊。
     *
     * @param lineStations      所有路線車站關聯資料
     * @param lineTransfers     所有路線轉乘資料
     * @param lineStationById   路線車站關聯 id 對應路線車站資料的 Map
     * @param lineStationByCode 車站代碼對應路線車站資料的 Map
     * @param strategy          路線策略（1：最少轉乘次數，2：最短車程時間）
     * @return 車站代碼對應其所有可達邊的鄰接表
     */
    public Map<String, List<Edge>> buildAdjacencyList(
            List<LineStation> lineStations,
            List<LineTransfer> lineTransfers,
            Map<Integer, LineStation> lineStationById,
            Map<String, LineStation> lineStationByCode,
            int strategy) {

        Map<String, List<Edge>> adjacencyList = new HashMap<>();

        // 同線相鄰邊
        Map<Short, List<LineStation>> byLine = lineStations.stream()
                .filter(lineStation -> lineStation.getLineId() != null)
                .collect(Collectors.groupingBy(LineStation::getLineId));

        for (List<LineStation> lineStationGroup : byLine.values()) {
            List<LineStation> sorted = lineStationGroup.stream()
                    .sorted(Comparator.comparingInt(
                            lineStation -> lineStation.getStationSequence() != null
                                    ? lineStation.getStationSequence()
                                    : 0))
                    .collect(Collectors.toList());

            for (int i = 0; i < sorted.size() - 1; i++) {
                LineStation currentStation = sorted.get(i);
                LineStation nextStation = sorted.get(i + 1);

                if (metroForkBranchRouteGraphService.isSecondaryBranchStation(currentStation.getStationCode())
                        || metroForkBranchRouteGraphService.isSecondaryBranchStation(nextStation.getStationCode())) {
                    continue;
                }

                // 策略 1 亦採用實際秒數（而非固定 0），使轉乘次數相同的多條路徑之間，
                // 以實際車程時間作為次要比較依據，避免任意選到多坐好幾站才轉乘的路徑（見轉乘邊權重註解）
                int currentTime = currentStation.getCumulativeTime() != null
                        ? currentStation.getCumulativeTime().intValue()
                        : 0;
                int nextTime = nextStation.getCumulativeTime() != null
                        ? nextStation.getCumulativeTime().intValue()
                        : 0;
                int weight = Math.abs(nextTime - currentTime);

                adjacencyList.computeIfAbsent(currentStation.getStationCode(), code -> new ArrayList<>())
                        .add(new Edge(nextStation.getStationCode(), weight, false));
                adjacencyList.computeIfAbsent(nextStation.getStationCode(), code -> new ArrayList<>())
                        .add(new Edge(currentStation.getStationCode(), weight, false));
            }
        }

        // 分岔路線同線邊
        metroForkBranchRouteGraphService.addBranchEdges(adjacencyList, lineStationByCode);

        // 轉乘邊
        for (LineTransfer lineTransfer : lineTransfers) {
            LineStation from = lineStationById.get(lineTransfer.getFromLineStationId());
            LineStation to = lineStationById.get(lineTransfer.getToLineStationId());
            if (from == null || to == null)
                continue;

            int transferSeconds = lineTransfer.getTransferTime() != null
                    ? lineTransfer.getTransferTime().intValue() * 60
                    : 0;
            int weight = (strategy == 1) ? TRANSFER_COUNT_DOMINANT_WEIGHT + transferSeconds : transferSeconds;

            adjacencyList.computeIfAbsent(from.getStationCode(), code -> new ArrayList<>())
                    .add(new Edge(to.getStationCode(), weight, true));
            adjacencyList.computeIfAbsent(to.getStationCode(), code -> new ArrayList<>())
                    .add(new Edge(from.getStationCode(), weight, true));
        }

        return adjacencyList;
    }

    /**
     * 依實體車站 id，收集該站在各路線下對應的所有車站代碼（轉乘站會有多筆，例如民權西路的 "R13"、"O11"）。
     * 供 {@link #findRoute} 將轉乘站的多個線別代碼視為等價起訖點使用。
     *
     * @param lineStationByCode 車站代碼對應路線車站資料的 Map
     * @param stationId         實體車站 id
     * @return 該實體車站在各路線下的所有車站代碼
     */
    public Set<String> collectStationCodesByStationId(Map<String, LineStation> lineStationByCode, Integer stationId) {
        return lineStationByCode.values().stream()
                .filter(lineStation -> stationId.equals(lineStation.getStationId()))
                .map(LineStation::getStationCode)
                .collect(Collectors.toSet());
    }

    /**
     * 以 Dijkstra 演算法搜尋最短路徑，並找出完整的車站代碼路徑。
     * fromCodes/toCodes 為同一實體車站在不同路線下的所有代碼（轉乘站有多筆），只要抵達其中任一代碼即視為到達，
     * 避免使用者選到轉乘站的特定線別代碼（如 "O11"）時，被迫多算一段轉乘到該代碼才算抵達終點。
     * 找不到可達路徑時拋出 {@link IllegalArgumentException}。
     *
     * @param adjacencyList 鄰接表
     * @param fromCodes     起始站的所有等價車站代碼
     * @param toCodes       終點站的所有等價車站代碼
     * @return 最短路徑搜尋結果
     */
    public DijkstraResult findRoute(
            Map<String, List<Edge>> adjacencyList, Set<String> fromCodes, Set<String> toCodes) {

        Map<String, Integer> distanceByCode = new HashMap<>();
        Map<String, String> prevCode = new HashMap<>();
        Map<String, Boolean> prevIsTransfer = new HashMap<>();

        PriorityQueue<Object[]> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(entry -> (int) entry[0]));
        for (String fromCode : fromCodes) { // 轉乘站的每個線別代碼都以距離 0 同時入隊，等同多起點 Dijkstra
            distanceByCode.put(fromCode, 0);
            priorityQueue.add(new Object[] { 0, fromCode });
        }

        String reachedCode = null; // 記錄實際抵達的目標代碼，可能與使用者請求的代碼不同（同站的另一線別代碼）
        while (!priorityQueue.isEmpty()) {
            Object[] currentEntry = priorityQueue.poll();
            int currentCost = (int) currentEntry[0];
            String currentCode = (String) currentEntry[1];

            if (toCodes.contains(currentCode)) { // 抵達終點站任一線別代碼即結束搜尋
                reachedCode = currentCode;
                break;
            }

            if (currentCost > distanceByCode.getOrDefault(currentCode, Integer.MAX_VALUE)) {
                continue;
            }

            for (Edge edge : adjacencyList.getOrDefault(currentCode, Collections.emptyList())) {
                int newCost = currentCost + edge.getWeight();

                if (newCost < distanceByCode.getOrDefault(edge.getToCode(), Integer.MAX_VALUE)) {
                    distanceByCode.put(edge.getToCode(), newCost);
                    prevCode.put(edge.getToCode(), currentCode);
                    prevIsTransfer.put(edge.getToCode(), edge.isTransfer());
                    priorityQueue.add(new Object[] { newCost, edge.getToCode() });
                }
            }
        }

        if (reachedCode == null) {
            throw new IllegalArgumentException("找不到從 " + fromCodes + " 到 " + toCodes + " 的行程路線");
        }

        List<String> path = new ArrayList<>();
        String currentCode = reachedCode;
        while (currentCode != null) {
            path.add(0, currentCode);
            currentCode = prevCode.get(currentCode);
        }

        return new DijkstraResult(path, prevIsTransfer);
    }

    /**
     * 依路徑與轉乘標記，將連續同線車站切分並組成各路線段清單。
     */
    public List<OriginDestinationDetailVO.RouteSegmentVO> buildRouteSegments(
            List<String> path,
            Map<String, Boolean> prevIsTransfer,
            Map<String, LineStation> lineStationByCode,
            Map<Short, Line> lineById,
            Map<Integer, Station> stationById,
            List<StationFacilities> stationFacilities) {

        List<OriginDestinationDetailVO.RouteSegmentVO> route = new ArrayList<>();
        List<String> segmentCodes = new ArrayList<>();
        segmentCodes.add(path.get(0));

        for (int i = 1; i < path.size(); i++) {
            String code = path.get(i);
            if (Boolean.TRUE.equals(prevIsTransfer.get(code))) {
                route.add(buildSegment(segmentCodes, lineStationByCode, lineById, stationById, stationFacilities));
                segmentCodes = new ArrayList<>();
            }
            segmentCodes.add(code);
        }
        route.add(buildSegment(segmentCodes, lineStationByCode, lineById, stationById, stationFacilities));

        return route;
    }

    /**
     * 計算全程總行駛時間（秒）。轉乘段以轉乘時間計，行駛段以相鄰站累計時間差計。
     */
    // 回傳加總後的原始總秒數，讓前端對總時間進位一次，而非各段分別進位後再相加
    public int calculateTotalTime(
            List<String> path,
            Map<String, Boolean> prevIsTransfer,
            Map<String, LineStation> lineStationByCode,
            Map<String, Short> transferTimeMap) {

        int totalTime = 0;
        for (int i = 1; i < path.size(); i++) {
            String prev = path.get(i - 1);
            String code = path.get(i);
            if (Boolean.TRUE.equals(prevIsTransfer.get(code))) {
                Short transferTime = transferTimeMap.get(prev + ":" + code);
                totalTime += (transferTime != null ? transferTime.intValue() * 60 : 0);
            } else {
                LineStation previousLineStation = lineStationByCode.get(prev);
                LineStation currentLineStation = lineStationByCode.get(code);
                if (previousLineStation != null && currentLineStation != null &&
                        previousLineStation.getCumulativeTime() != null
                        && currentLineStation.getCumulativeTime() != null) {
                    totalTime += Math.abs(
                            currentLineStation.getCumulativeTime().intValue()
                                    - previousLineStation.getCumulativeTime().intValue());
                }
            }
        }
        return totalTime;
    }

    /**
     * 計算全程轉乘時間加總（秒），僅加總轉乘邊的時間，已包含於 {@link #calculateTotalTime} 的結果內。
     */
    public int calculateTransferTime(
            List<String> path,
            Map<String, Boolean> prevIsTransfer,
            Map<String, Short> transferTimeMap) {

        int transferTime = 0;
        for (int i = 1; i < path.size(); i++) {
            String prev = path.get(i - 1);
            String code = path.get(i);
            if (Boolean.TRUE.equals(prevIsTransfer.get(code))) {
                Short time = transferTimeMap.get(prev + ":" + code);
                transferTime += (time != null ? time.intValue() * 60 : 0);
            }
        }
        return transferTime;
    }

    /**
     * 將同一路線的連續車站代碼清單組成一個路線段。
     */
    public OriginDestinationDetailVO.RouteSegmentVO buildSegment(
            List<String> codes,
            Map<String, LineStation> lineStationByCode,
            Map<Short, Line> lineById,
            Map<Integer, Station> stationById,
            List<StationFacilities> stationFacilities) {

        LineStation firstLineStation = lineStationByCode.get(codes.get(0));

        Short lineId = (firstLineStation != null) ? firstLineStation.getLineId() : null;
        Line line = (lineId != null) ? lineById.get(lineId) : null;

        List<OriginDestinationDetailVO.StationInfoVO> stationInfos = codes.stream()
                .map(code -> {
                    LineStation lineStation = lineStationByCode.get(code);
                    Station station = (lineStation != null) ? stationById.get(lineStation.getStationId()) : null;
                    OriginDestinationDetailVO.StationInfoVO stationInfoVO = new OriginDestinationDetailVO.StationInfoVO(
                            code,
                            station != null ? station.getNameZhTw() : null,
                            station != null ? station.getNameEn() : null);
                    applyFacilities(stationInfoVO, station, stationFacilities);
                    return stationInfoVO;
                })
                .collect(Collectors.toList());

        // 逐相鄰站對加總（而非首尾站 cumulative_time 直接相減），
        // 避免路徑行經 Y 字分岔（蘆洲／新北投／小碧潭）切換支線時，首尾站的累計時間基準不同而算出錯誤秒數
        int segmentTime = 0;
        for (int i = 1; i < codes.size(); i++) {
            LineStation previousLineStation = lineStationByCode.get(codes.get(i - 1));
            LineStation currentLineStation = lineStationByCode.get(codes.get(i));
            if (previousLineStation != null && currentLineStation != null
                    && previousLineStation.getCumulativeTime() != null
                    && currentLineStation.getCumulativeTime() != null) {
                segmentTime += Math.abs(
                        currentLineStation.getCumulativeTime().intValue()
                                - previousLineStation.getCumulativeTime().intValue());
            }
        }

        return new OriginDestinationDetailVO.RouteSegmentVO(
                line != null ? line.getLetter() : null,
                line != null ? line.getNameZhTw() : null,
                line != null ? line.getColor() : null,
                stationInfos,
                segmentTime);
    }

    /**
     * 依設備過濾清單，將對應欄位由 Station 填入 StationInfoVO。清單為空時不填入任何欄位。
     */
    public void applyFacilities(
            OriginDestinationDetailVO.StationInfoVO stationInfoVO,
            Station station,
            List<StationFacilities> facilities) {
        if (station == null || facilities == null || facilities.isEmpty()) {
            return;
        }

        for (StationFacilities facility : facilities) {
            switch (facility) {
                case ATM -> stationInfoVO.setAtm(station.getAtm());
                case NURSING_ROOM -> stationInfoVO.setNursingRoom(station.getNursingRoom());
                case DIAPER_TABLE -> stationInfoVO.setDiaperTable(station.getDiaperTable());
                case CHARGING_STATION -> stationInfoVO.setChargingStation(station.getChargingStation());
                case TICKET_MACHINE -> stationInfoVO.setTicketMachine(station.getTicketMachine());
                case LOCKER -> stationInfoVO.setLocker(station.getLocker());
                case DRINKING_WATER -> stationInfoVO.setDrinkingWater(station.getDrinkingWater());
                case RESTROOM -> stationInfoVO.setRestroom(station.getRestroom());
                case ELEVATOR -> stationInfoVO.setElevator(station.getElevator());
                case ESCALATOR -> stationInfoVO.setEscalator(station.getEscalator());
            }
        }
    }

}
