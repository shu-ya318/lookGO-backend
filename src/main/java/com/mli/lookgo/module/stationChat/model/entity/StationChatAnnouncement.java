package com.mli.lookgo.module.stationChat.model.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理站點聊天公告相關的資料。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@Schema(description = "處理站點聊天公告相關的資料")
public class StationChatAnnouncement {

    @Schema(description = "公告 id", example = "1")
    private Integer id;

    @Schema(description = "車站 id", example = "1")
    private Integer stationId;

    @Schema(description = "公告內容", example = "本站電梯本週維修暫停使用")
    private String content;

    @Schema(description = "建立者 user id", example = "1")
    private Integer createdBy;

    @Schema(description = "建立時間 (UTC, ISO 8601)", example = "2026-07-04T12:00:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-07-04T12:00:00Z")
    private LocalDateTime updatedAt;

    @Schema(description = "軟刪除時間 (UTC, ISO 8601)", example = "2026-07-04T12:00:00Z")
    private LocalDateTime deletedAt;

    public StationChatAnnouncement() {
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

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "StationChatAnnouncement{" +
                "id=" + id +
                ", stationId=" + stationId +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
