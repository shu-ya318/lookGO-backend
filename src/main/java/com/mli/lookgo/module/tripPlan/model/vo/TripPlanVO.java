package com.mli.lookgo.module.tripPlan.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於旅程規劃管理頁面顯示的物件，整合起訖站名稱資訊。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Schema(description = "用於旅程規劃管理頁面顯示的物件")
public class TripPlanVO {

    @Schema(description = "旅程規劃 id", example = "1")
    private Integer id;

    @Schema(description = "旅程名稱", example = "上班通勤路線")
    private String name;

    @Schema(description = "起站 id", example = "3")
    private Integer fromStationId;

    @Schema(description = "起站中文名稱", example = "淡水站")
    private String fromStationNameZhTw;

    @Schema(description = "訖站 id", example = "8")
    private Integer toStationId;

    @Schema(description = "訖站中文名稱", example = "台北車站")
    private String toStationNameZhTw;

    @Schema(description = "票種代碼 (1=全票, 4=學生, 5=兒童, 7=愛心)", example = "1")
    private Integer fareType;

    @Schema(description = "票價", example = "30")
    private BigDecimal farePrice;

    @Schema(description = "轉乘次數", example = "1")
    private Integer transferCount;

    @Schema(description = "路線規劃策略代碼 (1=最少轉乘次數, 2=最短車程時間)", example = "1")
    private Integer routingStrategy;

    @Schema(description = "總車程時間 (秒，含轉乘時間；依起訖站與路線規劃策略即時計算)", example = "1800")
    private Integer travelTimeSeconds;

    @Schema(description = "備註", example = "平日上班使用")
    private String notes;

    @Schema(description = "建立時間 (UTC, ISO 8601)", example = "2026-07-06T08:00:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "最後更新時間 (UTC, ISO 8601)", example = "2026-07-06T08:00:00Z")
    private LocalDateTime updatedAt;

    public TripPlanVO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getFromStationId() {
        return fromStationId;
    }

    public void setFromStationId(Integer fromStationId) {
        this.fromStationId = fromStationId;
    }

    public String getFromStationNameZhTw() {
        return fromStationNameZhTw;
    }

    public void setFromStationNameZhTw(String fromStationNameZhTw) {
        this.fromStationNameZhTw = fromStationNameZhTw;
    }

    public Integer getToStationId() {
        return toStationId;
    }

    public void setToStationId(Integer toStationId) {
        this.toStationId = toStationId;
    }

    public String getToStationNameZhTw() {
        return toStationNameZhTw;
    }

    public void setToStationNameZhTw(String toStationNameZhTw) {
        this.toStationNameZhTw = toStationNameZhTw;
    }

    public Integer getFareType() {
        return fareType;
    }

    public void setFareType(Integer fareType) {
        this.fareType = fareType;
    }

    public BigDecimal getFarePrice() {
        return farePrice;
    }

    public void setFarePrice(BigDecimal farePrice) {
        this.farePrice = farePrice;
    }

    public Integer getTransferCount() {
        return transferCount;
    }

    public void setTransferCount(Integer transferCount) {
        this.transferCount = transferCount;
    }

    public Integer getRoutingStrategy() {
        return routingStrategy;
    }

    public void setRoutingStrategy(Integer routingStrategy) {
        this.routingStrategy = routingStrategy;
    }

    public Integer getTravelTimeSeconds() {
        return travelTimeSeconds;
    }

    public void setTravelTimeSeconds(Integer travelTimeSeconds) {
        this.travelTimeSeconds = travelTimeSeconds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "TripPlanVO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", fromStationNameZhTw='" + fromStationNameZhTw + '\'' +
                ", toStationNameZhTw='" + toStationNameZhTw + '\'' +
                ", travelTimeSeconds=" + travelTimeSeconds +
                ", createdAt=" + createdAt +
                '}';
    }
}
