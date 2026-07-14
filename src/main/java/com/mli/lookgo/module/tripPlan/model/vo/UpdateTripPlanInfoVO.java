package com.mli.lookgo.module.tripPlan.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳旅程規劃資訊（起訖站以外）更新結果的物件。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳旅程規劃資訊（起訖站以外）更新結果的物件")
public class UpdateTripPlanInfoVO {

    @Schema(description = "旅程規劃 id", example = "1")
    private Integer id;

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

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-07-14T08:00:00Z")
    private LocalDateTime updatedAt;

    public UpdateTripPlanInfoVO() {
    }

    public UpdateTripPlanInfoVO(Integer id, Integer fareType, BigDecimal farePrice, Integer transferCount,
            Integer routingStrategy, String notes, LocalDateTime updatedAt) {
        this.id = id;
        this.fareType = fareType;
        this.farePrice = farePrice;
        this.transferCount = transferCount;
        this.routingStrategy = routingStrategy;
        this.notes = notes;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
