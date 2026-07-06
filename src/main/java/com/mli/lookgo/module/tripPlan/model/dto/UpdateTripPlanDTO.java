package com.mli.lookgo.module.tripPlan.model.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 處理更新旅程規劃資訊（起訖站以外）相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Schema(description = "處理更新旅程規劃資訊（起訖站以外）相關的資料傳輸物件")
public class UpdateTripPlanDTO {

    @Schema(description = "旅程規劃 id", example = "1")
    @NotNull(message = "請輸入旅程規劃id!")
    private Integer tripPlanId;

    @Schema(description = "票種代碼 (1=全票, 4=學生, 5=兒童, 7=愛心)", example = "1")
    @NotNull(message = "請輸入票種代碼!")
    private Integer fareType;

    @Schema(description = "票價", example = "30")
    @NotNull(message = "請輸入票價!")
    @DecimalMin(value = "0", message = "票價不可為負數!")
    private BigDecimal farePrice;

    @Schema(description = "轉乘次數", example = "1")
    @NotNull(message = "請輸入轉乘次數!")
    @Min(value = 0, message = "轉乘次數不可為負數!")
    private Integer transferCount;

    @Schema(description = "路線規劃策略代碼 (1=最少轉乘次數, 2=最短車程時間)", example = "1")
    @NotNull(message = "請輸入路線規劃策略代碼!")
    private Integer routingStrategy;

    @Schema(description = "備註", example = "平日上班使用")
    @Size(max = 2000, message = "備註長度不可超過2000字!")
    private String notes;

    public Integer getTripPlanId() {
        return tripPlanId;
    }

    public void setTripPlanId(Integer tripPlanId) {
        this.tripPlanId = tripPlanId;
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

    @Override
    public String toString() {
        return "UpdateTripPlanDTO{" +
                "tripPlanId=" + tripPlanId +
                ", fareType=" + fareType +
                ", farePrice=" + farePrice +
                ", transferCount=" + transferCount +
                ", routingStrategy=" + routingStrategy +
                '}';
    }
}
