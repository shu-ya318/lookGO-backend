package com.mli.lookgo.module.metro.model.graph;

/**
 * 處理捷運路網圖的一條邊，供 Dijkstra 演算法建圖與搜尋使用。
 *
 * @author D5042101
 * @since 2026.07.02
 */
public class Edge {

    // 目的車站代碼
    private final String toCode;

    // 邊的權重（依路線策略為 0、轉乘時間或行駛時間秒數）
    private final int weight;

    // 是否為轉乘邊
    private final boolean isTransfer;

    public Edge(String toCode, int weight, boolean isTransfer) {
        this.toCode = toCode;
        this.weight = weight;
        this.isTransfer = isTransfer;
    }

    public String getToCode() {
        return toCode;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isTransfer() {
        return isTransfer;
    }
}
