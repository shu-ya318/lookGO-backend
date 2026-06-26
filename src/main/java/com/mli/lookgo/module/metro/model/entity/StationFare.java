package com.mli.lookgo.module.metro.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理捷運票價相關的資料。
 *
 * @author D5042101
 * @since 2026.06.26
 */
@Schema(description = "處理捷運票價相關的資料")
public class StationFare {

    @Schema(description = "票價 id", example = "1")
    private Integer id;

    @Schema(description = "起站 id", example = "1")
    private Integer fromStationId;

    @Schema(description = "迄站 id", example = "2")
    private Integer toStationId;

    @Schema(description = "票種 (1=全票, 4=學生, 5=兒童, 7=愛心)", example = "1")
    private Integer fareType;

    @Schema(description = "票價 (元)", example = "20.00")
    private BigDecimal price;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-06-26T12:00:00Z")
    private LocalDateTime updatedAt;

    public StationFare() {
    }

    public StationFare(Integer fromStationId, Integer toStationId, Integer fareType,
            BigDecimal price, LocalDateTime updatedAt) {
        this.fromStationId = fromStationId;
        this.toStationId = toStationId;
        this.fareType = fareType;
        this.price = price;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFromStationId() {
        return fromStationId;
    }

    public void setFromStationId(Integer fromStationId) {
        this.fromStationId = fromStationId;
    }

    public Integer getToStationId() {
        return toStationId;
    }

    public void setToStationId(Integer toStationId) {
        this.toStationId = toStationId;
    }

    public Integer getFareType() {
        return fareType;
    }

    public void setFareType(Integer fareType) {
        this.fareType = fareType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "StationFare{" +
                "id=" + id +
                ", fromStationId=" + fromStationId +
                ", toStationId=" + toStationId +
                ", fareType=" + fareType +
                ", price=" + price +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
