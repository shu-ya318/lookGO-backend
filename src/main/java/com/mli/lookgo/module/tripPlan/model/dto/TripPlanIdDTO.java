package com.mli.lookgo.module.tripPlan.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 處理指定單筆旅程規劃相關的資料傳輸物件，供刪除、匯出 excel 使用。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Schema(description = "處理指定單筆旅程規劃相關的資料傳輸物件")
public class TripPlanIdDTO {

    @Schema(description = "旅程規劃 id", example = "1")
    @NotNull(message = "請輸入旅程規劃id!")
    private Integer tripPlanId;

    public Integer getTripPlanId() {
        return tripPlanId;
    }

    public void setTripPlanId(Integer tripPlanId) {
        this.tripPlanId = tripPlanId;
    }

    @Override
    public String toString() {
        return "TripPlanIdDTO{tripPlanId=" + tripPlanId + "}";
    }
}
