package com.mli.lookgo.module.metro.model.vo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 捷運路網地圖的複合回應資料，整合路線、車站與換乘資訊供前端 D3.js 繪圖使用。
 *
 * @author D5042101
 * @since 2026.06.26
 */
@Schema(description = "捷運路網地圖的複合回應資料")
public class MapVO {

    @Schema(description = "所有路線及其車站清單")
    private List<LineVO> lines;

    @Schema(description = "所有換乘連結清單")
    private List<TransferVO> transfers;

    public MapVO(List<LineVO> lines, List<TransferVO> transfers) {
        this.lines = lines;
        this.transfers = transfers;
    }

    public List<LineVO> getLines() {
        return lines;
    }

    public List<TransferVO> getTransfers() {
        return transfers;
    }

    // ----- 路線 -----

    @Schema(description = "路線及其依序排列的車站")
    public static class LineVO {

        @Schema(description = "路線代號", example = "BL")
        private String letter;

        @Schema(description = "路線顏色 (Hex)", example = "#0072C6")
        private String color;

        @Schema(description = "路線中文名稱", example = "板南線")
        private String nameZhTw;

        @Schema(description = "路線英文名稱", example = "Bannan Line")
        private String nameEn;

        @Schema(description = "路線上的車站清單，依 station_sequence 升序排列")
        private List<StationVO> stations;

        public LineVO(String letter, String color, String nameZhTw, String nameEn,
                List<StationVO> stations) {
            this.letter = letter;
            this.color = color;
            this.nameZhTw = nameZhTw;
            this.nameEn = nameEn;
            this.stations = stations;
        }

        public String getLetter() {
            return letter;
        }

        public String getColor() {
            return color;
        }

        public String getNameZhTw() {
            return nameZhTw;
        }

        public String getNameEn() {
            return nameEn;
        }

        public List<StationVO> getStations() {
            return stations;
        }
    }

    // ----- 車站 -----

    @Schema(description = "路線上的車站資訊")
    public static class StationVO {

        @Schema(description = "車站代碼", example = "BL01")
        private String stationCode;

        @Schema(description = "車站 id", example = "1")
        private Integer stationId;

        @Schema(description = "車站中文名稱", example = "頂埔")
        private String nameZhTw;

        @Schema(description = "車站英文名稱", example = "Dingpu")
        private String nameEn;

        @Schema(description = "車站在路線上的順序", example = "1")
        private Short sequence;

        public StationVO(String stationCode, Integer stationId, String nameZhTw,
                String nameEn, Short sequence) {
            this.stationCode = stationCode;
            this.stationId = stationId;
            this.nameZhTw = nameZhTw;
            this.nameEn = nameEn;
            this.sequence = sequence;
        }

        public String getStationCode() {
            return stationCode;
        }

        public Integer getStationId() {
            return stationId;
        }

        public String getNameZhTw() {
            return nameZhTw;
        }

        public String getNameEn() {
            return nameEn;
        }

        public Short getSequence() {
            return sequence;
        }
    }

    // ----- 換乘 -----

    @Schema(description = "兩路線間的換乘連結")
    public static class TransferVO {

        @Schema(description = "換乘起始站代碼", example = "BL12")
        private String fromStationCode;

        @Schema(description = "換乘目標站代碼", example = "R10")
        private String toStationCode;

        @Schema(description = "換乘所需時間 (分鐘)", example = "5")
        private Short transferTime;

        public TransferVO(String fromStationCode, String toStationCode, Short transferTime) {
            this.fromStationCode = fromStationCode;
            this.toStationCode = toStationCode;
            this.transferTime = transferTime;
        }

        public String getFromStationCode() {
            return fromStationCode;
        }

        public String getToStationCode() {
            return toStationCode;
        }

        public Short getTransferTime() {
            return transferTime;
        }
    }
}
