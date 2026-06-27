package com.mli.lookgo.module.metro.model.vo;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 對應 TDX Metro StationOfLine API 回傳的 JSON 結構。
 *
 * @author D5042101
 * @since 2026.06.25
 */
public class MetroLineStationVO {

    @JsonProperty("LineID")
    private String lineId;

    @JsonProperty("Direction")
    private Integer direction;

    @JsonProperty("Stations")
    private List<StationDetail> stations;

    public String getLineId() {
        return lineId;
    }

    public Integer getDirection() {
        return direction;
    }

    public List<StationDetail> getStations() {
        return stations;
    }

    public static class StationDetail {

        @JsonProperty("Sequence")
        private Short sequence;

        @JsonProperty("StationID")
        private String stationCode;

        @JsonProperty("StationName")
        private NameTranslation stationName;

        @JsonProperty("CumulativeDistance")
        private BigDecimal cumulativeDistance;

        @JsonProperty("TravelTime")
        private Short travelTime;

        public Short getSequence() {
            return sequence;
        }

        public String getStationCode() {
            return stationCode;
        }

        public String getStationNameZhTw() {
            return stationName != null ? stationName.getZhTw() : null;
        }

        public BigDecimal getCumulativeDistance() {
            return cumulativeDistance;
        }

        public Short getTravelTime() {
            return travelTime;
        }

        public static class NameTranslation {
            @JsonProperty("Zh_tw")
            private String zhTw;

            @JsonProperty("En")
            private String en;

            public String getZhTw() {
                return zhTw;
            }

            public String getEn() {
                return en;
            }
        }
    }
}
