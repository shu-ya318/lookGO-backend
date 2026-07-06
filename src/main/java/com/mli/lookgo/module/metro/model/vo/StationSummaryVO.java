package com.mli.lookgo.module.metro.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於車站管理頁面分頁列表顯示的輕量物件，僅包含列表所需的基本欄位。
 *
 * @author D5042101
 * @since 2026.07.05
 */
@Schema(description = "用於車站管理頁面分頁列表顯示的物件")
public class StationSummaryVO {

    @Schema(description = "車站 id", example = "1")
    private Integer id;

    @Schema(description = "車站中文名稱", example = "淡水站")
    private String nameZhTw;

    @Schema(description = "車站英文名稱", example = "Danshui")
    private String nameEn;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-06-25T12:00:00Z")
    private LocalDateTime updatedAt;

    public StationSummaryVO() {
    }

    public StationSummaryVO(Integer id, String nameZhTw, String nameEn, LocalDateTime updatedAt) {
        this.id = id;
        this.nameZhTw = nameZhTw;
        this.nameEn = nameEn;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNameZhTw() {
        return nameZhTw;
    }

    public void setNameZhTw(String nameZhTw) {
        this.nameZhTw = nameZhTw;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "StationSummaryVO{id=" + id + ", nameZhTw='" + nameZhTw + '\'' + ", nameEn='" + nameEn + '\'' + '}';
    }
}
