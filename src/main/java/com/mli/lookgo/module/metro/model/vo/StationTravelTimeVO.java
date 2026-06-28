package com.mli.lookgo.module.metro.model.vo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 對應 TDX Metro S2STravelTime API 回傳的 JSON 結構。
 * 提供相鄰車站間的行駛時間與停靠時間，用於計算各站累計行駛時間。
 *
 * @author D5042101
 * @since 2026.06.26
 */
public class StationTravelTimeVO {

    @JsonProperty("LineID")
    private String lineId;

    @JsonProperty("TravelTimes")
    private List<TravelTimeDetail> travelTimes;

    public String getLineId() {
        return lineId;
    }

    public List<TravelTimeDetail> getTravelTimes() {
        return travelTimes;
    }

    public static class TravelTimeDetail {

        @JsonProperty("Sequence")
        private Integer sequence;

        @JsonProperty("FromStationID")
        private String fromStationId;

        @JsonProperty("ToStationID")
        private String toStationId;

        @JsonProperty("RunTime")
        private Integer runTime;

        @JsonProperty("StopTime")
        private Integer stopTime;

        public Integer getSequence() {
            return sequence;
        }

        public String getFromStationId() {
            return fromStationId;
        }

        public String getToStationId() {
            return toStationId;
        }

        public Integer getRunTime() {
            return runTime;
        }

        public Integer getStopTime() {
            return stopTime;
        }
    }
}
