package com.mli.lookgo.module.metro.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳前端下拉選單所需車站選項的物件（車站代碼＋中文名稱）。
 *
 * @author D5042101
 * @since 2026.07.05
 */
@Schema(description = "回傳前端下拉選單所需車站選項的物件")
public class StationOptionVO {

    @Schema(description = "車站代碼", example = "R28")
    private String stationCode;

    @Schema(description = "車站中文名稱", example = "淡水")
    private String nameZhTw;

    @Schema(description = "路線顏色 (Hex)", example = "#E3002C")
    private String lineColor;

    public StationOptionVO() {
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getNameZhTw() {
        return nameZhTw;
    }

    public void setNameZhTw(String nameZhTw) {
        this.nameZhTw = nameZhTw;
    }

    public String getLineColor() {
        return lineColor;
    }

    public void setLineColor(String lineColor) {
        this.lineColor = lineColor;
    }

    @Override
    public String toString() {
        return "StationOptionVO{stationCode='" + stationCode + '\'' + ", nameZhTw='" + nameZhTw + '\''
                + ", lineColor='" + lineColor + '\'' + '}';
    }
}
