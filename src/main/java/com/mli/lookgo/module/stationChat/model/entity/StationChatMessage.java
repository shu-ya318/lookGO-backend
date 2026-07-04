package com.mli.lookgo.module.stationChat.model.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理車站聊天留言相關的資料。
 *
 * @author D5042101
 * @since 2026.07.04
 */
@Schema(description = "處理車站聊天留言相關的資料")
public class StationChatMessage {

    @Schema(description = "留言 id", example = "1")
    private Integer id;

    @Schema(description = "車站 id", example = "1")
    private Integer stationId;

    @Schema(description = "留言者 user id", example = "1")
    private Integer userId;

    @Schema(description = "旅程分享關聯的旅程規劃 id (chatType=2 時提供)", example = "10")
    private Integer tripPlanId;

    @Schema(description = "留言類型 (1=文字訊息, 2=旅程分享)", example = "1")
    private Integer chatType;

    @Schema(description = "文字訊息內容 (chatType=1 時提供)", example = "這裡的電梯正在維修")
    private String content;

    @Schema(description = "建立時間 (UTC, ISO 8601)", example = "2026-07-04T12:00:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "軟刪除時間 (UTC, ISO 8601)", example = "2026-07-04T12:00:00Z")
    private LocalDateTime deletedAt;

    public StationChatMessage() {
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getTripPlanId() {
        return tripPlanId;
    }

    public void setTripPlanId(Integer tripPlanId) {
        this.tripPlanId = tripPlanId;
    }

    public Integer getChatType() {
        return chatType;
    }

    public void setChatType(Integer chatType) {
        this.chatType = chatType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
        return "StationChatMessage{" +
                "id=" + id +
                ", stationId=" + stationId +
                ", userId=" + userId +
                ", tripPlanId=" + tripPlanId +
                ", chatType=" + chatType +
                ", createdAt=" + createdAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
