package com.mli.lookgo.module.stationBookmark.model.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理使用者車站書籤相關的資料。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Schema(description = "處理使用者車站書籤相關的資料")
public class UserStationBookmark {

    @Schema(description = "書籤 id", example = "1")
    private Integer id;

    @Schema(description = "車站 id", example = "3")
    private Integer stationId;

    @Schema(description = "使用者 id", example = "10")
    private Integer userId;

    @Schema(description = "收藏時間 (UTC, ISO 8601)", example = "2026-07-01T08:00:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "軟刪除時間 (UTC, ISO 8601)，未刪除則為 null")
    private LocalDateTime deletedAt;

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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "UserStationBookmark{" +
                "id=" + id +
                ", stationId=" + stationId +
                ", userId=" + userId +
                ", createdAt=" + createdAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
