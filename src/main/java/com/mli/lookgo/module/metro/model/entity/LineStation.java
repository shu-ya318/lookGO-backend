package com.mli.lookgo.module.metro.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理捷運所有路線車站相關的資料。(同車站保留隸屬於不同路線的多筆資料)
 *
 * @author D5042101
 * @since 2026.06.25
 */
@Schema(description = "處理捷運所有路線車站相關的資料")
public class LineStation {

    @Schema(description = "所有路線車站 id", example = "1")
    private Integer id;

    @Schema(description = "路線 id", example = "1")
    private Short lineId;

    @Schema(description = "車站 id", example = "1")
    private Integer stationId;

    @Schema(description = "車站順序", example = "1")
    private Short stationSequence;

    @Schema(description = "車站代碼", example = "BL01")
    private String stationCode;

    @Schema(description = "累計距離 (公里)", example = "12.50")
    private BigDecimal cumulativeDistance;

    @Schema(description = "累計行駛時間 (秒)", example = "221")
    private Short cumulativeTime;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-06-25T12:00:00Z")
    private LocalDateTime updatedAt;
}
