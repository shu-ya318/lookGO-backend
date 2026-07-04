package com.mli.lookgo.module.stationChat.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 處理編輯站點聊天公告相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@Schema(description = "處理編輯站點聊天公告相關的資料傳輸物件")
public class UpdateAnnouncementDTO {

    @Schema(description = "公告 id", example = "1")
    @NotNull(message = "請輸入公告 id!")
    private Integer announcementId;

    @Schema(description = "公告內容", example = "本站電梯已恢復正常使用")
    @NotBlank(message = "請輸入公告內容!")
    @Size(max = 1000, message = "公告內容長度不能超過 1000 個字元!")
    private String content;

    public Integer getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(Integer announcementId) {
        this.announcementId = announcementId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "UpdateAnnouncementDTO{announcementId=" + announcementId + ", content='" + content + '\'' + '}';
    }
}
