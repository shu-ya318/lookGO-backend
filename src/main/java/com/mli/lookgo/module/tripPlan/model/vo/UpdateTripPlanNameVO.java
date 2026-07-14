package com.mli.lookgo.module.tripPlan.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳旅程規劃名稱更新結果的物件。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳旅程規劃名稱更新結果的物件")
public class UpdateTripPlanNameVO {

    @Schema(description = "旅程規劃 id", example = "1")
    private Integer id;

    @Schema(description = "旅程名稱", example = "假日出遊路線")
    private String name;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-07-14T08:00:00Z")
    private LocalDateTime updatedAt;

    public UpdateTripPlanNameVO() {
    }

    public UpdateTripPlanNameVO(Integer id, String name, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.updatedAt = updatedAt;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
