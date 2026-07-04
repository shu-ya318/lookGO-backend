package com.mli.lookgo.module.stationChat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 處理依車站 id 取得站點聊天公告相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@Schema(description = "處理依車站 id 取得站點聊天公告相關的資料傳輸物件")
public class GetAnnouncementByStationIdDTO {

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
        return "GetAnnouncementByStationIdDTO{stationId=" + stationId + '}';
    }
}
