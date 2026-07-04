package com.mli.lookgo.module.stationChat.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 回傳車站聊天公告資料的物件。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@Schema(description = "回傳車站聊天公告資料的物件")
public class StationChatAnnouncementVO {

    @Schema(description = "公告 id", example = "1")
    private Integer id;

    @Schema(description = "車站 id", example = "1")
    private Integer stationId;

    @Schema(description = "公告內容", example = "本站電梯本週維修暫停使用")
    private String content;

    @Schema(description = "建立者名稱", example = "admin")
    private String createdByUsername;

    @Schema(description = "建立時間 (UTC, ISO 8601)", example = "2026-07-04T12:00:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-07-04T12:00:00Z")
    private LocalDateTime updatedAt;

    public StationChatAnnouncementVO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStationId() {
        return stationId;
    }

    public void setStationId(Integer stationId) {
        this.stationId = stationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "StationChatAnnouncementVO{" +
                "id=" + id +
                ", stationId=" + stationId +
                ", createdByUsername='" + createdByUsername + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
