package com.mli.lookgo.module.metro.model.graph;

import java.util.List;
import java.util.Map;

/**
 * Dijkstra 演算法搜尋結果，包含最短路徑車站代碼清單，及各節點是否由轉乘邊到達的標記表。
 *
 * @author D5042101
 * @since 2026.07.02
 */
public class DijkstraResult {

    // 最短路徑車站代碼清單，依序排列
    private final List<String> path;

    // 各車站代碼對應「是否由轉乘邊到達該站」的標記表
    private final Map<String, Boolean> prevIsTransfer;

    public DijkstraResult(List<String> path, Map<String, Boolean> prevIsTransfer) {
        this.path = path;
        this.prevIsTransfer = prevIsTransfer;
    }

    public List<String> getPath() {
        return path;
    }

    public Map<String, Boolean> getPrevIsTransfer() {
        return prevIsTransfer;
    }
}
