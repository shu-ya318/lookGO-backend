package com.mli.lookgo.module.stationChat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 處理刪除車站聊天公告相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@Schema(description = "處理刪除車站聊天公告相關的資料傳輸物件")
public class DeleteAnnouncementDTO {

    @Schema(description = "公告 id", example = "1")
    @NotNull(message = "請輸入公告 id!")
    private Integer announcementId;

    public Integer getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(Integer announcementId) {
        this.announcementId = announcementId;
    }

    @Override
    public String toString() {
        return "DeleteAnnouncementDTO{announcementId=" + announcementId + '}';
    }
}
