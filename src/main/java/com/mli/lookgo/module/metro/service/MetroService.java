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
     * 整合路線、車站與換乘資料，組成供前端 D3.js 繪圖使用的捷運路網地圖資料。
     *
     * @return MapVO
     */
    public MapVO getMetroMap() {
        logger.debug("開始組合捷運路網地圖資料");

        List<Line> lines = metroDAO.getAllLine();
        List<Station> stations = metroDAO.getAllStation();
        List<LineStation> lineStations = metroDAO.getAllLineStation();
        List<LineTransfer> lineTransfers = metroDAO.getAllLineTransfer();

        // station_id → Station
        Map<Integer, Station> stationById = stations.stream()
                .collect(Collectors.toMap(Station::getId, s -> s, (a, b) -> a));

        // lines_stations.id → station_code (換乘對應用)
        Map<Integer, String> lineStationIdToCode = lineStations.stream()
                .filter(ls -> ls.getId() != null && ls.getStationCode() != null)
                .collect(Collectors.toMap(LineStation::getId, LineStation::getStationCode, (a, b) -> a));

        // line_id → List<LineStation>，依 station_sequence 升序
        Map<Short, List<LineStation>> lineStationsByLineId = lineStations.stream()
                .filter(ls -> ls.getLineId() != null)
                .collect(Collectors.groupingBy(LineStation::getLineId));

        // 組合 LineVO 清單
        List<MapVO.LineVO> lineVOs = lines.stream()
                .map(line -> {
                    List<MapVO.StationVO> stationVOs = lineStationsByLineId
                            .getOrDefault(line.getId(), Collections.emptyList())
                            .stream()
                            .sorted(Comparator.comparingInt(
                                    ls -> ls.getStationSequence() != null ? ls.getStationSequence() : 0))
                            .map(ls -> {
                                Station s = stationById.get(ls.getStationId());
                                return new MapVO.StationVO(
                                        ls.getStationCode(),
                                        ls.getStationId(),
                                        s != null ? s.getNameZhTw() : null,
                                        s != null ? s.getNameEn() : null,
                                        ls.getStationSequence());
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

        // 組合 TransferVO 清單 (fromStationCode / toStationCode 任一對應不到時跳過)
        List<MapVO.TransferVO> transferVOs = lineTransfers.stream()
                .map(lt -> new MapVO.TransferVO(
                        lineStationIdToCode.get(lt.getFromLineStationId()),
                        lineStationIdToCode.get(lt.getToLineStationId()),
                        lt.getTransferTime()))
                .filter(tv -> tv.getFromStationCode() != null && tv.getToStationCode() != null)
                .collect(Collectors.toList());

        logger.debug("捷運路網地圖資料組合完成，共 {} 條路線、{} 個換乘連結",
                lineVOs.size(), transferVOs.size());

        return new MapVO(lineVOs, transferVOs);
    }

    /**
     * 依起始、終點車站代碼取得兩站間詳細資料，可選擇性指定票種與路線策略。
     * <p>
     * 路線策略 1（最少轉乘次數）：以 Dijkstra 搜尋，轉乘邊權重為 1、同線邊權重為 0。<br>
     * 路線策略 2（最短車程時間）：以 Dijkstra 搜尋，各邊權重為實際秒數。
     *
     * @param stationRouteDTO
     * @return OriginDestinationDetailVO
     */
    public OriginDestinationDetailVO getOriginDestinationDetail(StationRouteDTO stationRouteDTO) {
        String fromCode = stationRouteDTO.getFromStationCode();
        String toCode = stationRouteDTO.getToStationCode();
        Integer fareType = stationRouteDTO.getFareType();
        Integer routingStrategy = stationRouteDTO.getRoutingStrategy();

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

        List<Line> lines = metroDAO.getAllLine();
        List<Station> stations = metroDAO.getAllStation();
        List<LineStation> lineStations = metroDAO.getAllLineStation();
        List<LineTransfer> lineTransfers = metroDAO.getAllLineTransfer();

        Map<Short, Line> lineById = lines.stream()
                .filter(l -> l.getId() != null)
                .collect(Collectors.toMap(Line::getId, l -> l));
        Map<Integer, Station> stationById = stations.stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(Station::getId, s -> s));
        Map<String, LineStation> lsByCode = lineStations.stream()
                .filter(ls -> ls.getStationCode() != null)
                .collect(Collectors.toMap(LineStation::getStationCode, ls -> ls));
        Map<Integer, LineStation> lsById = lineStations.stream()
                .filter(ls -> ls.getId() != null)
                .collect(Collectors.toMap(LineStation::getId, ls -> ls));

        if (!lsByCode.containsKey(fromCode)) {
            throw new StationNotFoundException("找不到代碼:" + fromCode + "的車站!");
        }
        if (!lsByCode.containsKey(toCode)) {
            throw new StationNotFoundException("找不到代碼:" + toCode + "的車站!");
        }

        if (fromCode.equals(toCode)) {
            return buildSameStationResult(fromCode, fareType, strategy, lsByCode, lineById, stationById);
        }

        Map<String, Short> transferTimeMap = buildTransferTimeMap(lineTransfers, lsById);
        Map<String, List<Edge>> adj = buildAdjacencyList(lineStations, lineTransfers, lsById, strategy);
        DijkstraResult dijkstraResult = findRoute(adj, fromCode, toCode);

        List<OriginDestinationDetailVO.RouteSegmentVO> route = buildRouteSegments(
                dijkstraResult.path(), dijkstraResult.prevIsTransfer(), lsByCode, lineById, stationById);
        int transferCount = (int) dijkstraResult.path().stream()
                .filter(c -> Boolean.TRUE.equals(dijkstraResult.prevIsTransfer().get(c)))
                .count();
        int totalTime = calculateTotalTime(
                dijkstraResult.path(), dijkstraResult.prevIsTransfer(), lsByCode, transferTimeMap);
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
            Map<String, LineStation> lsByCode,
            Map<Short, Line> lineById,
            Map<Integer, Station> stationById) {

        LineStation ls = lsByCode.get(stationCode);
        Station s = stationById.get(ls.getStationId());
        Line line = lineById.get(ls.getLineId());

        OriginDestinationDetailVO.StationInfoVO stationInfo = new OriginDestinationDetailVO.StationInfoVO(
                stationCode,
                s != null ? s.getNameZhTw() : null,
                s != null ? s.getNameEn() : null);
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
     * 雙向皆建立，確保正反向皆可查詢。
     */
    private Map<String, Short> buildTransferTimeMap(
            List<LineTransfer> lineTransfers,
            Map<Integer, LineStation> lsById) {

        Map<String, Short> transferTimeMap = new HashMap<>();
        for (LineTransfer lt : lineTransfers) {
            LineStation from = lsById.get(lt.getFromLineStationId());
            LineStation to = lsById.get(lt.getToLineStationId());
            if (from == null || to == null) continue;
            Short tt = lt.getTransferTime();
            transferTimeMap.put(from.getStationCode() + ":" + to.getStationCode(), tt);
            transferTimeMap.put(to.getStationCode() + ":" + from.getStationCode(), tt);
        }
        return transferTimeMap;
    }

    /**
     * 依路線策略建立鄰接表。
     * <p>
     * 策略 1：同線邊權重 0、換乘邊權重 1（最小化轉乘次數）。<br>
     * 策略 2：同線邊權重為相鄰站累計時間秒數差、換乘邊權重為換乘時間秒數（最小化車程時間）。
     */
    private Map<String, List<Edge>> buildAdjacencyList(
            List<LineStation> lineStations,
            List<LineTransfer> lineTransfers,
            Map<Integer, LineStation> lsById,
            int strategy) {

        Map<String, List<Edge>> adj = new HashMap<>();

        // 同線相鄰邊
        Map<Short, List<LineStation>> byLine = lineStations.stream()
                .filter(ls -> ls.getLineId() != null)
                .collect(Collectors.groupingBy(LineStation::getLineId));

        for (List<LineStation> lsList : byLine.values()) {
            List<LineStation> sorted = lsList.stream()
                    .sorted(Comparator.comparingInt(
                            ls -> ls.getStationSequence() != null ? ls.getStationSequence() : 0))
                    .collect(Collectors.toList());

            for (int i = 0; i < sorted.size() - 1; i++) {
                LineStation a = sorted.get(i);
                LineStation b = sorted.get(i + 1);

                int weight = 0;
                if (strategy == 2) {
                    int ta = a.getCumulativeTime() != null ? a.getCumulativeTime().intValue() : 0;
                    int tb = b.getCumulativeTime() != null ? b.getCumulativeTime().intValue() : 0;
                    weight = Math.abs(tb - ta);
                }

                adj.computeIfAbsent(a.getStationCode(), k -> new ArrayList<>())
                        .add(new Edge(b.getStationCode(), weight, false));
                adj.computeIfAbsent(b.getStationCode(), k -> new ArrayList<>())
                        .add(new Edge(a.getStationCode(), weight, false));
            }
        }

        // 換乘邊
        for (LineTransfer lt : lineTransfers) {
            LineStation from = lsById.get(lt.getFromLineStationId());
            LineStation to = lsById.get(lt.getToLineStationId());
            if (from == null || to == null) continue;

            int weight = (strategy == 1) ? 1
                    : (lt.getTransferTime() != null ? lt.getTransferTime().intValue() * 60 : 0);

            adj.computeIfAbsent(from.getStationCode(), k -> new ArrayList<>())
                    .add(new Edge(to.getStationCode(), weight, true));
            adj.computeIfAbsent(to.getStationCode(), k -> new ArrayList<>())
                    .add(new Edge(from.getStationCode(), weight, true));
        }

        return adj;
    }

    /**
     * 以 Dijkstra 演算法搜尋最短路徑，並回溯出完整的車站代碼路徑。
     * 找不到可達路徑時拋出 {@link IllegalArgumentException}。
     */
    private DijkstraResult findRoute(
            Map<String, List<Edge>> adj, String fromCode, String toCode) {

        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prevCode = new HashMap<>();
        Map<String, Boolean> prevIsTransfer = new HashMap<>();

        PriorityQueue<Object[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> (int) a[0]));
        dist.put(fromCode, 0);
        pq.add(new Object[] { 0, fromCode });

        while (!pq.isEmpty()) {
            Object[] curr = pq.poll();
            int currCost = (int) curr[0];
            String currCode = (String) curr[1];

            if (currCode.equals(toCode)) break;
            if (currCost > dist.getOrDefault(currCode, Integer.MAX_VALUE)) continue;

            for (Edge edge : adj.getOrDefault(currCode, Collections.emptyList())) {
                int newCost = currCost + edge.weight;
                if (newCost < dist.getOrDefault(edge.toCode, Integer.MAX_VALUE)) {
                    dist.put(edge.toCode, newCost);
                    prevCode.put(edge.toCode, currCode);
                    prevIsTransfer.put(edge.toCode, edge.isTransfer);
                    pq.add(new Object[] { newCost, edge.toCode });
                }
            }
        }

        if (!dist.containsKey(toCode)) {
            throw new IllegalArgumentException("找不到從 " + fromCode + " 到 " + toCode + " 的行程路線");
        }

        List<String> path = new ArrayList<>();
        String curr = toCode;
        while (curr != null) {
            path.add(0, curr);
            curr = prevCode.get(curr);
        }

        return new DijkstraResult(path, prevIsTransfer);
    }

    /**
     * 依路徑與換乘標記，將連續同線車站切分並組成各路線段清單。
     */
    private List<OriginDestinationDetailVO.RouteSegmentVO> buildRouteSegments(
            List<String> path,
            Map<String, Boolean> prevIsTransfer,
            Map<String, LineStation> lsByCode,
            Map<Short, Line> lineById,
            Map<Integer, Station> stationById) {

        List<OriginDestinationDetailVO.RouteSegmentVO> route = new ArrayList<>();
        List<String> segmentCodes = new ArrayList<>();
        segmentCodes.add(path.get(0));

        for (int i = 1; i < path.size(); i++) {
            String code = path.get(i);
            if (Boolean.TRUE.equals(prevIsTransfer.get(code))) {
                route.add(buildSegment(segmentCodes, lsByCode, lineById, stationById));
                segmentCodes = new ArrayList<>();
            }
            segmentCodes.add(code);
        }
        route.add(buildSegment(segmentCodes, lsByCode, lineById, stationById));

        return route;
    }

    /**
     * 計算全程總行駛時間（秒）。換乘段以換乘時間計，行駛段以相鄰站累計時間差計。
     */
    private int calculateTotalTime(
            List<String> path,
            Map<String, Boolean> prevIsTransfer,
            Map<String, LineStation> lsByCode,
            Map<String, Short> transferTimeMap) {

        int totalTime = 0;
        for (int i = 1; i < path.size(); i++) {
            String prev = path.get(i - 1);
            String code = path.get(i);
            if (Boolean.TRUE.equals(prevIsTransfer.get(code))) {
                Short tt = transferTimeMap.get(prev + ":" + code);
                totalTime += (tt != null ? tt.intValue() * 60 : 0);
            } else {
                LineStation prevLS = lsByCode.get(prev);
                LineStation currLS = lsByCode.get(code);
                if (prevLS != null && currLS != null &&
                        prevLS.getCumulativeTime() != null && currLS.getCumulativeTime() != null) {
                    totalTime += Math.abs(
                            currLS.getCumulativeTime().intValue() - prevLS.getCumulativeTime().intValue());
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
            Map<String, LineStation> lsByCode,
            Map<Short, Line> lineById,
            Map<Integer, Station> stationById) {

        LineStation firstLS = lsByCode.get(codes.get(0));
        LineStation lastLS = lsByCode.get(codes.get(codes.size() - 1));

        Short lineId = (firstLS != null) ? firstLS.getLineId() : null;
        Line line = (lineId != null) ? lineById.get(lineId) : null;

        List<OriginDestinationDetailVO.StationInfoVO> stationInfos = codes.stream()
                .map(code -> {
                    LineStation ls = lsByCode.get(code);
                    Station s = (ls != null) ? stationById.get(ls.getStationId()) : null;
                    return new OriginDestinationDetailVO.StationInfoVO(
                            code,
                            s != null ? s.getNameZhTw() : null,
                            s != null ? s.getNameEn() : null);
                })
                .collect(Collectors.toList());

        int segmentTime = 0;
        if (firstLS != null && lastLS != null &&
                firstLS.getCumulativeTime() != null && lastLS.getCumulativeTime() != null) {
            segmentTime = Math.abs(
                    lastLS.getCumulativeTime().intValue() - firstLS.getCumulativeTime().intValue());
        }

        return new OriginDestinationDetailVO.RouteSegmentVO(
                line != null ? line.getLetter() : null,
                line != null ? line.getNameZhTw() : null,
                line != null ? line.getColor() : null,
                stationInfos,
                segmentTime);
    }

    // ----- 私有輔助型別 -----

    private record DijkstraResult(List<String> path, Map<String, Boolean> prevIsTransfer) {}

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
