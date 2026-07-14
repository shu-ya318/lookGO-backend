package com.mli.lookgo.module.stationChat.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳車站聊天公告更新結果的物件。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳車站聊天公告更新結果的物件")
public class UpdateAnnouncementVO {

    @Schema(description = "公告 id", example = "1")
    private Integer id;

    @Schema(description = "公告內容", example = "本站電梯已恢復正常使用")
    private String content;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-07-14T08:00:00Z")
    private LocalDateTime updatedAt;

    public UpdateAnnouncementVO() {
    }

    public UpdateAnnouncementVO(Integer id, String content, LocalDateTime updatedAt) {
        this.id = id;
        this.content = content;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
