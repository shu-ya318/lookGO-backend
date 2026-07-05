package com.mli.lookgo.module.stationChat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 處理依車站 id 匯出車站當日聊天紀錄相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.05
 */
@Schema(description = "處理依車站 id 匯出車站當日聊天紀錄相關的資料傳輸物件")
public class StationIdDTO {

    @Schema(description = "車站 id", example = "1")
    @NotNull(message = "請輸入車站 id!")
    private Integer stationId;

    public Integer getStationId() {
        return stationId;
    }

    public void setStationId(Integer stationId) {
        this.stationId = stationId;
    }

    @Override
    public String toString() {
        return "StationIdDTO{stationId=" + stationId + '}';
    }
}
