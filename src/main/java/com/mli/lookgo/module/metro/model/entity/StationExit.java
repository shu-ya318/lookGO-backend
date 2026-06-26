package com.mli.lookgo.module.metro.model.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理捷運車站出口相關的資料。
 *
 * @author D5042101
 * @since 2026.06.26
 */
@Schema(description = "處理捷運車站出口相關的資料")
public class StationExit {

    @Schema(description = "出口 id", example = "1")
    private Integer id;

    @Schema(description = "車站 id", example = "1")
    private Integer stationId;

    @Schema(description = "電梯資訊", example = "出口1旁")
    private String elevator;

    @Schema(description = "電扶梯資訊", example = "出口1旁")
    private String escalator;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-06-26T12:00:00Z")
    private LocalDateTime updatedAt;

    public StationExit() {
    }

    public StationExit(Integer stationId, String elevator, String escalator, LocalDateTime updatedAt) {
        this.stationId = stationId;
        this.elevator = elevator;
        this.escalator = escalator;
        this.updatedAt = updatedAt;
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

    public String getElevator() {
        return elevator;
    }

    public void setElevator(String elevator) {
        this.elevator = elevator;
    }

    public String getEscalator() {
        return escalator;
    }

    public void setEscalator(String escalator) {
        this.escalator = escalator;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "StationExit{" +
                "id=" + id +
                ", stationId=" + stationId +
                ", elevator='" + elevator + '\'' +
                ", escalator='" + escalator + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
