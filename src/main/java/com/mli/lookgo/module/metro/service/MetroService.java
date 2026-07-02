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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mli.lookgo.module.metro.dao.MetroDAO;
import com.mli.lookgo.module.metro.enums.StationFacilities;
import com.mli.lookgo.module.metro.exceptions.StationNotFoundException;
import com.mli.lookgo.module.metro.model.dto.StationRouteDTO;
import com.mli.lookgo.module.metro.model.dto.StationDetailsDTO;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.entity.StationFare;
import com.mli.lookgo.module.metro.model.vo.MapVO;
import com.mli.lookgo.module.metro.model.vo.OriginDestinationDetailVO;
import com.mli.lookgo.module.metro.model.vo.StationDetailVO;

/**
 * 處理前端查詢捷運資料相關的業務邏輯。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@Service
public class MetroService {

    private final MetroDAO metroDAO;
    private static final Logger logger = LoggerFactory.getLogger(MetroService.class);
    private static final Set<Integer> VALID_FARE_TYPES = Set.of(1, 4, 5, 7);
    private static final Set<Integer> VALID_ROUTING_STRATEGIES = Set.of(1, 2);
    private static final BigDecimal SAME_STATION_FARE = BigDecimal.valueOf(20);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param metroDAO
     */
    public MetroService(MetroDAO metroDAO) {
        this.metroDAO = metroDAO;
    }

    /**
     * 取得所有路線資料。
     *
     * @return List<Line>
     */
    public List<Line> getAllLine() {
        logger.debug("開始查詢所有路線資料");
        return metroDAO.getAllLine();
    }

    /**
     * 取得所有車站資料。
     *
     * @return List<Station>
     */
    public List<Station> getAllStation() {
        logger.debug("開始查詢所有車站資料");
        return metroDAO.getAllStation();
    }

    /**
     * 取得所有路線車站資料。
     *
     * @return List<LineStation>
     */
    public List<LineStation> getAllLineStation() {
        logger.debug("開始查詢所有路線車站資料");
        return metroDAO.getAllLineStation();
    }

    /**
     * 取得所有票價資料。
     *
     * @return List<StationFare>
     */
    public List<StationFare> getAllStationFare() {
        logger.debug("開始查詢所有票價資料");
        return metroDAO.getAllStationFare();
    }

    /**
     * 取得所有路線換乘資料。
     *
     * @return List<LineTransfer>
     */
    public List<LineTransfer> getAllLineTransfer() {
        logger.debug("開始查詢所有路線換乘資料");
        return metroDAO.getAllLineTransfer();
    }

    /**
     * 依車站代碼取得車站詳細資料。
     *
     * @param stationDetailsDTO
     * @return StationDetailVO
     */
    public StationDetailVO getStationByCode(StationDetailsDTO stationDetailsDTO) {
        if (!metroDAO.existsByStationCode(stationDetailsDTO.getStationCode())) {
            throw new StationNotFoundException("找不到代碼:" + stationDetailsDTO.getStationCode() + "的車站!");
        }

        logger.debug("開始依車站代碼查詢車站詳細資料，stationCode: {}", stationDetailsDTO.getStationCode());
        return metroDAO.getStationByCode(stationDetailsDTO);
    }

    /**
     * 整合路線、車站與換乘資料，組成供前端繪製路網地圖的資料。
     *
     * @return MapVO
     */
    public MapVO getMetroMap() {
        logger.debug("開始組合捷運路網地圖資料");

        // 1. 從資料庫讀取所有捷運路線、車站、路線車站關聯及轉乘步行時間等基礎資料
        List<Line> lines = metroDAO.getAllLine();
        List<Station> stations = metroDAO.getAllStation();
        List<LineStation> lineStations = metroDAO.getAllLineStation();
        List<LineTransfer> lineTransfers = metroDAO.getAllLineTransfer();

        // 2. 建立「車站 ID -> 車站實體」對照 Map，以利後續透過 ID 查找車站的中英文名稱。例如: {101 -> 淡水車站}
        Map<Integer, Station> stationById = stations.stream()
                .collect(Collectors.toMap(Station::getId, station -> station,
                        (existingValue, newValue) -> existingValue));

        // 3. 建立「路線車站關聯表 ID -> 車站代碼」的對照 Map，供轉乘起迄點代碼轉換使用。例如: {30 -> "BL12"}
        Map<Integer, String> lineStationIdToCode = lineStations.stream()
                .filter(lineStation -> lineStation.getId() != null && lineStation.getStationCode() != null)
                .collect(Collectors.toMap(LineStation::getId, LineStation::getStationCode,
                        (existingValue, newValue) -> existingValue));

        // 4. 將所有路線車站關聯依路線 ID 進行分組，以便按路線快速查出其底下的所有站點。例如: {2 -> [淡水, 紅樹林]}
        Map<Short, List<LineStation>> lineStationsByLineId = lineStations.stream()
                .filter(lineStation -> lineStation.getLineId() != null)
                .collect(Collectors.groupingBy(LineStation::getLineId));

        // 5. 遍歷並整合各路線與其依站點順序排序後的車站清單，轉換為前端地圖所需的 LineVO 結構（例如：紅線包含排序好的淡水、紅樹林等車站）
        List<MapVO.LineVO> lineVOs = lines.stream()
                .map(line -> {
                    List<MapVO.StationVO> stationVOs = lineStationsByLineId
                            .getOrDefault(line.getId(), Collections.emptyList())
                            .stream()
                            .sorted(Comparator.comparingInt(
                                    lineStation -> lineStation.getStationSequence() != null
                                            ? lineStation.getStationSequence()
                                            : 0))
                            .map(lineStation -> {
                                Station station = stationById.get(lineStation.getStationId());
                                return new MapVO.StationVO(
                                        lineStation.getStationCode(),
                                        lineStation.getStationId(),
                                        station != null ? station.getNameZhTw() : null,
                                        station != null ? station.getNameEn() : null,
                                        lineStation.getStationSequence());
                            })
                            .collect(Collectors.toList());

                    return new MapVO.LineVO(
                            line.getLetter(),
                            line.getColor(),
                            line.getNameZhTw(),
                            line.getNameEn(),
                            stationVOs);
                })
                .collect(Collectors.toList());

        // 6. 整合轉乘對照資料，將資料庫關聯表 ID 轉換為起迄站點代碼與步行時間（例如：台北車站板南線 "BL12" 到淡水信義線 "R10" 步行需 180
        // 秒）
        List<MapVO.TransferVO> transferVOs = lineTransfers.stream()
                .map(lineTransfer -> new MapVO.TransferVO(
                        lineStationIdToCode.get(lineTransfer.getFromLineStationId()),
                        lineStationIdToCode.get(lineTransfer.getToLineStationId()),
                        lineTransfer.getTransferTime()))
                .filter(transferVO -> transferVO.getFromStationCode() != null
                        && transferVO.getToStationCode() != null)
                .collect(Collectors.toList());

        logger.debug("捷運路網地圖資料組合完成，共 {} 條路線、{} 個換乘連結",
                lineVOs.size(), transferVOs.size());

        return new MapVO(lineVOs, transferVOs);
    }

    /**
     * 依起始、終點車站代碼取得兩站間詳細資料，並可選擇性指定票種與路線策略。
     * 因捷運路網為多線交織、站點間存在多條可能路徑的圖狀結構，使用 Dijkstra 演算法搜尋兩站間的最短路徑。
     * 路線策略 1（最少轉乘次數）：轉乘邊權重為 1、同線邊權重為 0，搜尋結果為轉乘次數最少的路徑。
     * 路線策略 2（最短車程時間）：各邊權重為實際秒數，搜尋結果為車程時間最短的路徑。
     *
     * @param stationRouteDTO
     * @return OriginDestinationDetailVO
     */
    public OriginDestinationDetailVO getOriginDestinationDetail(StationRouteDTO stationRouteDTO) {
        String fromCode = stationRouteDTO.getFromStationCode();
        String toCode = stationRouteDTO.getToStationCode();
        Integer fareType = stationRouteDTO.getFareType();
        Integer routingStrategy = stationRouteDTO.getRoutingStrategy();
        List<StationFacilities> stationFacilities = stationRouteDTO.getStationFacilities();

        // 1. 驗證傳入的票種與路線規劃策略是否合法，不合法則拋出異常（例如：僅接受全票、學生票等與最少轉乘、最短時間策略）
        if (fareType != null && !VALID_FARE_TYPES.contains(fareType)) {
            throw new IllegalArgumentException(
                    "不支援的票種: " + fareType + "，有效值為 1(全票)、4(學生)、5(兒童)、7(愛心)");
        }
        if (routingStrategy != null && !VALID_ROUTING_STRATEGIES.contains(routingStrategy)) {
            throw new IllegalArgumentException(
                    "不支援的路線策略: " + routingStrategy + "，有效值為 1(最少轉乘次數)、2(最短車程時間)");
        }

        int strategy = routingStrategy != null ? routingStrategy : 1;

        logger.debug("開始查詢起終點站詳細資料，fromStationCode: {}，toStationCode: {}，strategy: {}",
                fromCode, toCode, strategy);

        // 2. 自資料庫取得捷運路線、車站、關聯及轉乘等基本資料，並建立對應的快速查找 Map（例如：將 "R28" 代碼映射到淡水站實體）
        List<Line> lines = metroDAO.getAllLine();
        List<Station> stations = metroDAO.getAllStation();
        List<LineStation> lineStations = metroDAO.getAllLineStation();
        List<LineTransfer> lineTransfers = metroDAO.getAllLineTransfer();

        Map<Short, Line> lineById = lines.stream()
                .filter(line -> line.getId() != null)
                .collect(Collectors.toMap(Line::getId, line -> line));

        Map<Integer, Station> stationById = stations.stream()
                .filter(station -> station.getId() != null)
                .collect(Collectors.toMap(Station::getId, station -> station));

        Map<String, LineStation> lineStationByCode = lineStations.stream()
                .filter(lineStation -> lineStation.getStationCode() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, lineStation -> lineStation));

        Map<Integer, LineStation> lineStationById = lineStations.stream()
                .filter(lineStation -> lineStation.getId() != null)
                .collect(Collectors.toMap(LineStation::getId, lineStation -> lineStation));

        // 3. 確認起點與終點代碼在捷運路網中是否存在，不存在則拋出車站找不到異常
        if (!lineStationByCode.containsKey(fromCode)) {
            throw new StationNotFoundException("找不到代碼:" + fromCode + "的車站!");
        }
        if (!lineStationByCode.containsKey(toCode)) {
            throw new StationNotFoundException("找不到代碼:" + toCode + "的車站!");
        }

        // 4. 若起迄站代碼相同（例如起迄站皆為淡水站 "R28"），則直接組裝並回傳單一車站的結果，不再執行路徑搜尋
        if (fromCode.equals(toCode)) {
            return buildSameStationResult(fromCode, fareType, strategy, lineStationByCode, lineById, stationById,
                    stationFacilities);
        }

        // 5. 根據規劃策略（最少轉乘或最短車程）建立鄰接權重表，並執行 Dijkstra 演算法尋找最短路徑（例如：規劃從 "R28" 淡水到 "BL12"
        // 台北車站的路徑）
        Map<String, Short> transferTimeMap = buildTransferTimeMap(lineTransfers, lineStationById);
        Map<String, List<Edge>> adjacencyList = buildAdjacencyList(lineStations, lineTransfers, lineStationById,
                strategy);
        DijkstraResult dijkstraResult = findRoute(adjacencyList, fromCode, toCode);

        // 6. 將 Dijkstra 搜尋結果重組為前端所需的路線分段、計算轉乘次數、累加總乘車時間，並至資料庫撈取兩站間對應票價（例如：計算淡水至台北車站的票價為
        // 50 元）
        List<OriginDestinationDetailVO.RouteSegmentVO> route = buildRouteSegments(
                dijkstraResult.path, dijkstraResult.prevIsTransfer, lineStationByCode, lineById, stationById,
                stationFacilities);
        int transferCount = (int) dijkstraResult.path.stream()
                .filter(code -> Boolean.TRUE.equals(dijkstraResult.prevIsTransfer.get(code)))
                .count();
        int totalTime = calculateTotalTime(
                dijkstraResult.path, dijkstraResult.prevIsTransfer, lineStationByCode, transferTimeMap);
        BigDecimal farePrice = fareType != null
                ? metroDAO.getFareByStationCodesAndType(fromCode, toCode, fareType)
                : null;

        logger.debug("起終點站詳細資料查詢完成，轉乘次數: {}，總行駛時間: {} 秒", transferCount, totalTime);

        return new OriginDestinationDetailVO(fromCode, toCode, fareType, strategy,
                route, transferCount, totalTime, farePrice);
    }

    /**
     * 起終點相同時，直接組成只含單一車站的回傳結果。
     * farePrice 固定為同站票價 {@link #SAME_STATION_FARE}；未傳入 fareType 時不計算票價。
     */
    private OriginDestinationDetailVO buildSameStationResult(
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

        BigDecimal farePrice = fareType != null ? SAME_STATION_FARE : null;

        return new OriginDestinationDetailVO(stationCode, stationCode, fareType, strategy,
                Collections.singletonList(segment), 0, 0, farePrice);
    }

    /**
     * 建立換乘時間查找表，key 格式為 "fromCode:toCode"，value 為換乘時間（分鐘）。
     * 雙向皆建立，確保正反向路線皆可查詢。
     */
    private Map<String, Short> buildTransferTimeMap(
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
     * 策略 1：同線邊權重 0、換乘邊權重 1（最小化轉乘次數）。<br>
     * 策略 2：同線邊權重為相鄰站累計時間秒數差、換乘邊權重為換乘時間秒數（最小化車程時間）。
     */
    private Map<String, List<Edge>> buildAdjacencyList(
            List<LineStation> lineStations,
            List<LineTransfer> lineTransfers,
            Map<Integer, LineStation> lineStationById,
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

                int weight = 0;
                if (strategy == 2) {
                    int currentTime = currentStation.getCumulativeTime() != null
                            ? currentStation.getCumulativeTime().intValue()
                            : 0;
                    int nextTime = nextStation.getCumulativeTime() != null
                            ? nextStation.getCumulativeTime().intValue()
                            : 0;
                    weight = Math.abs(nextTime - currentTime);
                }

                adjacencyList.computeIfAbsent(currentStation.getStationCode(), code -> new ArrayList<>())
                        .add(new Edge(nextStation.getStationCode(), weight, false));
                adjacencyList.computeIfAbsent(nextStation.getStationCode(), code -> new ArrayList<>())
                        .add(new Edge(currentStation.getStationCode(), weight, false));
            }
        }

        // 換乘邊
        for (LineTransfer lineTransfer : lineTransfers) {
            LineStation from = lineStationById.get(lineTransfer.getFromLineStationId());
            LineStation to = lineStationById.get(lineTransfer.getToLineStationId());
            if (from == null || to == null)
                continue;

            int weight = (strategy == 1) ? 1
                    : (lineTransfer.getTransferTime() != null ? lineTransfer.getTransferTime().intValue() * 60 : 0);

            adjacencyList.computeIfAbsent(from.getStationCode(), code -> new ArrayList<>())
                    .add(new Edge(to.getStationCode(), weight, true));
            adjacencyList.computeIfAbsent(to.getStationCode(), code -> new ArrayList<>())
                    .add(new Edge(from.getStationCode(), weight, true));
        }

        return adjacencyList;
    }

    /**
     * 以 Dijkstra 演算法搜尋最短路徑，並找出完整的車站代碼路徑。
     * 找不到可達路徑時拋出 {@link IllegalArgumentException}。
     */
    private DijkstraResult findRoute(
            Map<String, List<Edge>> adjacencyList, String fromCode, String toCode) {

        Map<String, Integer> distanceByCode = new HashMap<>();
        Map<String, String> prevCode = new HashMap<>();
        Map<String, Boolean> prevIsTransfer = new HashMap<>();

        PriorityQueue<Object[]> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(entry -> (int) entry[0]));
        distanceByCode.put(fromCode, 0);
        priorityQueue.add(new Object[] { 0, fromCode });

        while (!priorityQueue.isEmpty()) {
            Object[] currentEntry = priorityQueue.poll();
            int currentCost = (int) currentEntry[0];
            String currentCode = (String) currentEntry[1];

            if (currentCode.equals(toCode))
                break;
            if (currentCost > distanceByCode.getOrDefault(currentCode, Integer.MAX_VALUE))
                continue;

            for (Edge edge : adjacencyList.getOrDefault(currentCode, Collections.emptyList())) {
                int newCost = currentCost + edge.weight;
                if (newCost < distanceByCode.getOrDefault(edge.toCode, Integer.MAX_VALUE)) {
                    distanceByCode.put(edge.toCode, newCost);
                    prevCode.put(edge.toCode, currentCode);
                    prevIsTransfer.put(edge.toCode, edge.isTransfer);
                    priorityQueue.add(new Object[] { newCost, edge.toCode });
                }
            }
        }

        if (!distanceByCode.containsKey(toCode)) {
            throw new IllegalArgumentException("找不到從 " + fromCode + " 到 " + toCode + " 的行程路線");
        }

        List<String> path = new ArrayList<>();
        String currentCode = toCode;
        while (currentCode != null) {
            path.add(0, currentCode);
            currentCode = prevCode.get(currentCode);
        }

        return new DijkstraResult(path, prevIsTransfer);
    }

    /**
     * 依路徑與換乘標記，將連續同線車站切分並組成各路線段清單。
     */
    private List<OriginDestinationDetailVO.RouteSegmentVO> buildRouteSegments(
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
     * 計算全程總行駛時間（秒）。換乘段以換乘時間計，行駛段以相鄰站累計時間差計。
     */
    private int calculateTotalTime(
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
     * 將同一路線的連續車站代碼清單組成一個路線段。
     */
    private OriginDestinationDetailVO.RouteSegmentVO buildSegment(
            List<String> codes,
            Map<String, LineStation> lineStationByCode,
            Map<Short, Line> lineById,
            Map<Integer, Station> stationById,
            List<StationFacilities> stationFacilities) {

        LineStation firstLineStation = lineStationByCode.get(codes.get(0));
        LineStation lastLineStation = lineStationByCode.get(codes.get(codes.size() - 1));

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

        int segmentTime = 0;
        if (firstLineStation != null && lastLineStation != null &&
                firstLineStation.getCumulativeTime() != null && lastLineStation.getCumulativeTime() != null) {
            segmentTime = Math.abs(
                    lastLineStation.getCumulativeTime().intValue() - firstLineStation.getCumulativeTime().intValue());
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
    private void applyFacilities(
            OriginDestinationDetailVO.StationInfoVO stationInfoVO,
            Station station,
            List<StationFacilities> facilities) {
        if (station == null || facilities == null || facilities.isEmpty()) {
            return;
        }

        for (StationFacilities facility : facilities) {
            switch (facility) {
                case ATM -> stationInfoVO.setAtm(station.getAtm());
                case NURSING_ROOM -> {
                    stationInfoVO.setNursingRoom(station.getNursingRoom());
                    stationInfoVO.setDiaperTable(station.getDiaperTable());
                }
                case CHARGING_STATION -> stationInfoVO.setChargingStation(station.getChargingStation());
                case TICKET_MACHINE -> stationInfoVO.setTicketMachine(station.getTicketMachine());
                case LOCKER -> stationInfoVO.setLocker(station.getLocker());
                case DRINKING_WATER -> stationInfoVO.setDrinkingWater(station.getDrinkingWater());
                case TOILET -> stationInfoVO.setRestroom(station.getRestroom());
                case ELEVATOR -> {
                    stationInfoVO.setElevator(station.getElevator());
                    stationInfoVO.setEscalator(station.getEscalator());
                }
                case ACCESSIBLE_FACILITIES -> stationInfoVO.setElevator(station.getElevator());
            }
        }
    }

    // ----- 私有輔助型別 -----

    private static class DijkstraResult {

        final List<String> path;
        final Map<String, Boolean> prevIsTransfer;

        DijkstraResult(List<String> path, Map<String, Boolean> prevIsTransfer) {
            this.path = path;
            this.prevIsTransfer = prevIsTransfer;
        }
    }

    private static class Edge {

        final String toCode;
        final int weight;
        final boolean isTransfer;

        Edge(String toCode, int weight, boolean isTransfer) {
            this.toCode = toCode;
            this.weight = weight;
            this.isTransfer = isTransfer;
        }
    }
}
