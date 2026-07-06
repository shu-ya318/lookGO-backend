package com.mli.lookgo.module.tripPlan.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 處理更新旅程規劃名稱相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Schema(description = "處理更新旅程規劃名稱相關的資料傳輸物件")
public class UpdateTripPlanNameDTO {

    @Schema(description = "旅程規劃 id", example = "1")
    @NotNull(message = "請輸入旅程規劃id!")
    private Integer tripPlanId;

    @Schema(description = "旅程名稱", example = "假日出遊路線")
    @NotBlank(message = "請輸入旅程名稱!")
    @Size(max = 100, message = "旅程名稱長度不可超過100字!")
    private String name;

    public Integer getTripPlanId() {
        return tripPlanId;
    }

    public void setTripPlanId(Integer tripPlanId) {
        this.tripPlanId = tripPlanId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "UpdateTripPlanNameDTO{tripPlanId=" + tripPlanId + ", name='" + name + "'}";
    }
}
