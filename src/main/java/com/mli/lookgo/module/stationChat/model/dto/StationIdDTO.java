package com.mli.lookgo.module.stationChat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 處理依車站 id 匯出車站當日聊天紀錄相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.05
 */
// schema 指定命名 ，避免因預設用類別名稱，致 Swagger 在掃描時發生名稱相同衝突
@Schema(name = "StationChatStationIdDTO", description = "處理依車站 id 匯出車站當日聊天紀錄相關的資料傳輸物件")
public class StationIdDTO {

    // 因模組是 stationChat，所以 id 前面加上 station
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
