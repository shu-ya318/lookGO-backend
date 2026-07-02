package com.mli.lookgo.module.metro.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.graph.Edge;

/**
 * 處理捷運分岔路線的業務邏輯。lines_stations 資料表以 (line_id, station_sequence) 唯一遞增排序，
 * 無法表達 Y 字分岔拓樸，{@link MetroRouteGraphService#buildAdjacencyList} 依 stationSequence 線性推導同線邊時，
 * 會在分岔口產生錯誤連接（例如把兩條支線硬串成一直線）。此類別改以人工維護的分岔站序覆蓋該區段，取代線性推導邏輯。
 * 目前涵蓋：
 * - 中和新蘆線（橘線 O）：大橋頭(O12) 分岔，蘆洲(O54)／迴龍(O21) 兩條支線
 * - 淡水信義線（紅線 R）：北投(R22) 單站支線，往新北投(R22A)
 * - 松山新店線（綠線 G）：七張(G03) 單站支線，往小碧潭(G03A)
 *
 * @author D5042101
 * @since 2026.07.02
 */
@Service
public class MetroForkBranchRouteGraphService {

    private final List<BranchLineDefinition> branchLineDefinitions = List.of(
            new BranchLineDefinition("O", List.of(
                    List.of("O12", "O50", "O51", "O52", "O53", "O54"),
                    List.of("O12", "O13", "O14", "O15", "O16", "O17", "O18", "O19", "O20", "O21"))),
            new BranchLineDefinition("R", List.of(
                    List.of("R22", "R22A"))),
            new BranchLineDefinition("G", List.of(
                    List.of("G03", "G03A"))));

    /**
     * TDX 對分岔站的 station_sequence 通常是附加在該路線既有序號之後（例如新北投 R22A 的序號緊接淡水 R28 之後），
     * 導致依序號排序後，分岔站在陣列中被錯誤地判定為與某個不相干的站（如 R28）相鄰。
     * 因此每條分岔路線中，除了分岔口本身（各支線清單的第一站，如大橋頭 O12／北投 R22／七張 G03）之外，
     * 其餘車站（secondary）一律排除於線性推導之外，一律由 {@link #addBranchEdges} 建立邊。
     * 分岔口本身仍保留線性推導，因為它與幹線上「非分岔站」鄰站（如民權西路 O11、奇岩 R21、小新店 G02）的邊是正確且未被涵蓋的。
     */
    private final Set<String> secondaryBranchStationCodes = branchLineDefinitions.stream()
            .flatMap(definition -> {
                String forkStationCode = definition.branches().get(0).get(0);
                return definition.branches().stream()
                        .flatMap(List::stream)
                        .filter(code -> !code.equals(forkStationCode));
            })
            .collect(Collectors.toCollection(HashSet::new));

    /**
     * 判斷指定車站代碼是否為分岔路線中「非分岔口」的支線車站（例如新北投 R22A、三重國小 O50）。
     * 若同線相鄰站對中任一站為此類車站，{@link MetroRouteGraphService#buildAdjacencyList} 需略過依
     * stationSequence 推導的原生同線邊，改交由 {@link #addBranchEdges} 建立正確邊，避免因 TDX 序號排列方式
     * 產生錯誤的線性連接（例如新北投 R22A 被誤連到淡水 R28）。
     *
     * @param stationCode 車站代碼
     * @return 是否為分岔路線中非分岔口的支線車站
     */
    public boolean isSecondaryBranchStation(String stationCode) {
        return secondaryBranchStationCodes.contains(stationCode);
    }

    /**
     * 依所有分岔路線定義建立同線邊，取代 buildAdjacencyList 依 stationSequence 排序推導的線性連接方式。
     * 邊權重計算方式與一般同線邊一致：策略 2 採相鄰站累計時間秒數差，策略 1 固定為 0。
     *
     * @param adjacencyList     鄰接表，會直接在此 Map 加入分岔邊
     * @param lineStationByCode 車站代碼對應的路線車站資料，供查詢累計時間以計算權重
     * @param strategy          路線策略（1：最少轉乘次數，2：最短車程時間）
     */
    public void addBranchEdges(
            Map<String, List<Edge>> adjacencyList,
            Map<String, LineStation> lineStationByCode,
            int strategy) {

        for (BranchLineDefinition definition : branchLineDefinitions) {
            for (List<String> branch : definition.branches()) {
                for (int i = 0; i < branch.size() - 1; i++) {
                    String fromCode = branch.get(i);
                    String toCode = branch.get(i + 1);
                    int weight = strategy == 2 ? calculateSegmentWeight(lineStationByCode, fromCode, toCode) : 0;

                    adjacencyList.computeIfAbsent(fromCode, code -> new ArrayList<>())
                            .add(new Edge(toCode, weight, false));
                    adjacencyList.computeIfAbsent(toCode, code -> new ArrayList<>())
                            .add(new Edge(fromCode, weight, false));
                }
            }
        }
    }

    /**
     * 計算分岔邊的行駛秒數，取相鄰兩站累計時間差的絕對值，任一站缺累計時間資料時回傳 0。
     */
    private int calculateSegmentWeight(Map<String, LineStation> lineStationByCode, String fromCode, String toCode) {
        LineStation from = lineStationByCode.get(fromCode);
        LineStation to = lineStationByCode.get(toCode);
        if (from == null || to == null || from.getCumulativeTime() == null || to.getCumulativeTime() == null) {
            return 0;
        }
        return Math.abs(to.getCumulativeTime().intValue() - from.getCumulativeTime().intValue());
    }

    /**
     * 單一路線的分岔站序定義，branches 為由分岔口出發的各支線車站代碼清單（依站序排列）。
     */
    private record BranchLineDefinition(String lineLetter, List<List<String>> branches) {
    }
}
