package com.mli.lookgo.module.stationBookmark.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於車站書籤管理頁面顯示的物件，整合車站與使用者資訊。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Schema(description = "用於車站書籤管理頁面顯示的物件")
public class StationBookmarkVO {

    @Schema(description = "書籤 id", example = "1")
    private Integer id;

    @Schema(description = "車站 id", example = "3")
    private Integer stationId;

    @Schema(description = "車站中文名稱", example = "淡水站")
    private String stationNameZhTw;

    @Schema(description = "車站英文名稱", example = "Tamsui")
    private String stationNameEn;

    @Schema(description = "使用者 id", example = "10")
    private Integer userId;

    @Schema(description = "使用者名稱", example = "小明")
    private String username;

    @Schema(description = "使用者 email", example = "user@example.com")
    private String email;

    @Schema(description = "收藏時間 (UTC, ISO 8601)", example = "2026-07-01T08:00:00Z")
    private LocalDateTime createdAt;

    public StationBookmarkVO() {
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

    public String getStationNameZhTw() {
        return stationNameZhTw;
    }

    public void setStationNameZhTw(String stationNameZhTw) {
        this.stationNameZhTw = stationNameZhTw;
    }

    public String getStationNameEn() {
        return stationNameEn;
    }

    public void setStationNameEn(String stationNameEn) {
        this.stationNameEn = stationNameEn;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "StationBookmarkVO{" +
                "id=" + id +
                ", stationNameZhTw='" + stationNameZhTw + '\'' +
                ", username='" + username + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
