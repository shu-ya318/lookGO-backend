package com.mli.lookgo.module.metro.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mli.lookgo.module.metro.dao.MetroDAO;
import com.mli.lookgo.module.metro.model.dto.OriginDestinationDetailDTO;
import com.mli.lookgo.module.metro.model.dto.StationDetailDTO;
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
     * @param stationDetailDTO
     * @return StationDetailVO
     */
    public StationDetailVO getStationByCode(StationDetailDTO stationDetailDTO) {
        logger.debug("開始依車站代碼查詢車站詳細資料，stationCode: {}", stationDetailDTO.getStationCode());
        return metroDAO.getStationByCode(stationDetailDTO.getStationCode());
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
     * @param originDestinationDetailDTO
     * @return OriginDestinationDetailVO
     */
    public OriginDestinationDetailVO getOriginDestinationDetail(
            OriginDestinationDetailDTO originDestinationDetailDTO) {
        int strategy = originDestinationDetailDTO.getRoutingStrategy() != null
                ? originDestinationDetailDTO.getRoutingStrategy() : 1;
        String fromCode = originDestinationDetailDTO.getFromStationCode();
        String toCode = originDestinationDetailDTO.getToStationCode();

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
            throw new IllegalArgumentException("起始車站代碼不存在: " + fromCode);
        }
        if (!lsByCode.containsKey(toCode)) {
            throw new IllegalArgumentException("終點車站代碼不存在: " + toCode);
        }

        // 起終點相同，直接回傳單站結果
        if (fromCode.equals(toCode)) {
            LineStation ls = lsByCode.get(fromCode);
            Station s = stationById.get(ls.getStationId());
            Line line = lineById.get(ls.getLineId());
            OriginDestinationDetailVO.StationInfoVO stationInfo = new OriginDestinationDetailVO.StationInfoVO(
                    fromCode,
                    s != null ? s.getNameZhTw() : null,
                    s != null ? s.getNameEn() : null);
            OriginDestinationDetailVO.RouteSegmentVO segment = new OriginDestinationDetailVO.RouteSegmentVO(
                    line != null ? line.getLetter() : null,
                    line != null ? line.getNameZhTw() : null,
                    line != null ? line.getColor() : null,
                    Collections.singletonList(stationInfo), 0);
            return new OriginDestinationDetailVO(fromCode, toCode,
                    originDestinationDetailDTO.getFareType(), strategy,
                    Collections.singletonList(segment), 0, 0, BigDecimal.ZERO);
        }

        // 換乘時間表: "fromCode:toCode" -> transferTime (分鐘)
        Map<String, Short> transferTimeMap = new HashMap<>();
        for (LineTransfer lt : lineTransfers) {
            LineStation from = lsById.get(lt.getFromLineStationId());
            LineStation to = lsById.get(lt.getToLineStationId());
            if (from == null || to == null) continue;
            Short tt = lt.getTransferTime();
            transferTimeMap.put(from.getStationCode() + ":" + to.getStationCode(), tt);
            transferTimeMap.put(to.getStationCode() + ":" + from.getStationCode(), tt);
        }

        // 建立鄰接表
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

            int weight;
            if (strategy == 1) {
                weight = 1;
            } else {
                int tm = lt.getTransferTime() != null ? lt.getTransferTime().intValue() : 0;
                weight = tm * 60;
            }

            adj.computeIfAbsent(from.getStationCode(), k -> new ArrayList<>())
                    .add(new Edge(to.getStationCode(), weight, true));
            adj.computeIfAbsent(to.getStationCode(), k -> new ArrayList<>())
                    .add(new Edge(from.getStationCode(), weight, true));
        }

        // Dijkstra
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

        // 回溯路徑
        List<String> path = new ArrayList<>();
        String curr = toCode;
        while (curr != null) {
            path.add(0, curr);
            curr = prevCode.get(curr);
        }

        // 建立路線段清單
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

        // 統計轉乘次數
        int transferCount = (int) path.stream()
                .filter(c -> Boolean.TRUE.equals(prevIsTransfer.get(c)))
                .count();

        // 計算總行駛時間（秒）
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

        // 查詢票價（選填）
        BigDecimal farePrice = null;
        if (originDestinationDetailDTO.getFareType() != null) {
            farePrice = metroDAO.getFareByStationCodesAndType(
                    fromCode, toCode, originDestinationDetailDTO.getFareType());
        }

        logger.debug("起終點站詳細資料查詢完成，轉乘次數: {}，總行駛時間: {} 秒", transferCount, totalTime);

        return new OriginDestinationDetailVO(fromCode, toCode,
                originDestinationDetailDTO.getFareType(), strategy,
                route, transferCount, totalTime, farePrice);
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

    // Dijkstra 邊（鄰接表用）
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
