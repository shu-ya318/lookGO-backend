package com.mli.lookgo.module.metro.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 對應 TDX Metro Station API 回傳的 JSON 結構。
 *
 * @author D5042101
 * @since 2026.06.25
 */
public class StationVO {

    @JsonProperty("StationID")
    private String stationSequence;

    @JsonProperty("StationName")
    private NameTranslation stationName;

    public String getStationSequence() {
        return stationSequence;
    }

    // 目的: 讓外部直接取得扁平化欄位
    public String getNameZhTw() {
        return stationName != null ? stationName.getZhTw() : null;
    }

    public String getNameEn() {
        return stationName != null ? stationName.getEn() : null;
    }

    // 目的: 讓 Jackson 正確解析巢狀 JSON 物件
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
