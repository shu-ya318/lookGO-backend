package com.mli.lookgo.module.tripPlan.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理使用者旅程規劃相關的資料。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Schema(description = "處理使用者旅程規劃相關的資料")
public class UserTripPlan {

    @Schema(description = "旅程規劃 id", example = "1")
    private Integer id;

    @Schema(description = "使用者 id", example = "10")
    private Integer userId;

    @Schema(description = "起站 id", example = "3")
    private Integer fromStationId;

    @Schema(description = "訖站 id", example = "8")
    private Integer toStationId;

    @Schema(description = "旅程名稱", example = "上班通勤路線")
    private String name;

    @Schema(description = "票種代碼 (1=全票, 4=學生, 5=兒童, 7=愛心)", example = "1")
    private Integer fareType;

    @Schema(description = "票價", example = "30")
    private BigDecimal farePrice;

    @Schema(description = "轉乘次數", example = "1")
    private Integer transferCount;

    @Schema(description = "路線規劃策略代碼 (1=最少轉乘次數, 2=最短車程時間)", example = "1")
    private Integer routingStrategy;

    @Schema(description = "備註", example = "平日上班使用")
    private String notes;

    @Schema(description = "建立時間 (UTC, ISO 8601)", example = "2026-07-06T08:00:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "最後更新時間 (UTC, ISO 8601)", example = "2026-07-06T08:00:00Z")
    private LocalDateTime updatedAt;

    @Schema(description = "軟刪除時間 (UTC, ISO 8601)，未刪除則為 null")
    private LocalDateTime deletedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getFromStationId() {
        return fromStationId;
    }

    public void setFromStationId(Integer fromStationId) {
        this.fromStationId = fromStationId;
    }

    public Integer getToStationId() {
        return toStationId;
    }

    public void setToStationId(Integer toStationId) {
        this.toStationId = toStationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "UserTripPlan{" +
                "id=" + id +
                ", userId=" + userId +
                ", fromStationId=" + fromStationId +
                ", toStationId=" + toStationId +
                ", name='" + name + '\'' +
                ", fareType=" + fareType +
                ", createdAt=" + createdAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
