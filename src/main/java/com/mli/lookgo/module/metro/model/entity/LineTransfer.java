package com.mli.lookgo.module.metro.model.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理捷運路線轉乘相關的資料。
 *
 * @author D5042101
 * @since 2026.06.26
 */
@Schema(description = "處理捷運路線轉乘相關的資料")
public class LineTransfer {

    @Schema(description = "轉乘 id", example = "1")
    private Integer id;

    @Schema(description = "起始路線車站 id (lines_stations.id)", example = "7")
    private Integer fromLineStationId;

    @Schema(description = "目標路線車站 id (lines_stations.id)", example = "100")
    private Integer toLineStationId;

    @Schema(description = "轉乘所需時間 (分鐘)", example = "11")
    private Short transferTime;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-06-26T12:00:00Z")
    private LocalDateTime updatedAt;

    public LineTransfer() {
    }

    public LineTransfer(Integer fromLineStationId, Integer toLineStationId,
            Short transferTime, LocalDateTime updatedAt) {
        this.fromLineStationId = fromLineStationId;
        this.toLineStationId = toLineStationId;
        this.transferTime = transferTime;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFromLineStationId() {
        return fromLineStationId;
    }

    public void setFromLineStationId(Integer fromLineStationId) {
        this.fromLineStationId = fromLineStationId;
    }

    public Integer getToLineStationId() {
        return toLineStationId;
    }

    public void setToLineStationId(Integer toLineStationId) {
        this.toLineStationId = toLineStationId;
    }

    public Short getTransferTime() {
        return transferTime;
    }

    public void setTransferTime(Short transferTime) {
        this.transferTime = transferTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "LineTransfer{" +
                "id=" + id +
                ", fromLineStationId=" + fromLineStationId +
                ", toLineStationId=" + toLineStationId +
                ", transferTime=" + transferTime +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
