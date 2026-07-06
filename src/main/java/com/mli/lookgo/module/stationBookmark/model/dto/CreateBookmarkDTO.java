package com.mli.lookgo.module.stationBookmark.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 處理新增車站書籤相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Schema(description = "處理新增車站書籤相關的資料傳輸物件")
public class CreateBookmarkDTO {

    @Schema(description = "車站 id", example = "3")
    @NotNull(message = "請輸入車站id!")
    private Integer stationId;

    public Integer getStationId() {
        return stationId;
    }

    public void setStationId(Integer stationId) {
        this.stationId = stationId;
    }

    @Override
    public String toString() {
        return "CreateBookmarkDTO{stationId=" + stationId + "}";
    }
}
