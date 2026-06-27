package com.mli.lookgo.module.metro.model.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理捷運路線相關的資料。
 *
 * @author D5042101
 * @since 2026.06.25
 */
@Schema(description = "處理捷運路線相關的資料")
public class Line {

    @Schema(description = "路線 id", example = "1")
    private Short id;

    @Schema(description = "路線代號", example = "BR")
    private String letter;

    @Schema(description = "路線中文名稱", example = "文湖線")
    private String nameZhTw;

    @Schema(description = "路線英文名稱", example = "Wenhu Line")
    private String nameEn;

    @Schema(description = "路線顏色", example = "#C48C31")
    private String color;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-06-24T12:00:00Z")
    private LocalDateTime updatedAt;

    public Line() {
    }

    public Line(String letter, String nameZhTw, String nameEn, String color,
            LocalDateTime updatedAt) {
        this.letter = letter;
        this.nameZhTw = nameZhTw;
        this.nameEn = nameEn;
        this.color = color;
        this.updatedAt = updatedAt;
    }

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Line{" +
                "id=" + id +
                ", letter='" + letter + '\'' +
                ", nameZhTw='" + nameZhTw + '\'' +
                ", nameEn='" + nameEn + '\'' +
                ", color='" + color + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
