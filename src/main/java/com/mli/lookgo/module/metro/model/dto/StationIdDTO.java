package com.mli.lookgo.module.metro.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 處理依車站 id 查詢車站詳細資料的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.05
 */
@Schema(name = "MetroStationIdDTO", description = "處理依車站 id 查詢車站詳細資料的資料傳輸物件")
public class StationIdDTO {

    @Schema(description = "車站 id", example = "1")
    @NotNull(message = "請輸入車站id!")
    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "StationIdDTO{id=" + id + "}";
    }
}
