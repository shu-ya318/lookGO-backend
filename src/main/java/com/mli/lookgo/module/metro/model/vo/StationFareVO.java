package com.mli.lookgo.module.metro.model.vo;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 對應 TDX Metro 票價 (StationFare) API 回傳的 JSON 結構。
 * 提供任意兩站間的票種與票價，用於同步票價資料。
 * CitizenCode 城市優惠票 (FareClass=3) 因資料表無對應欄位，同步時跳過。
 *
 * @author D5042101
 * @since 2026.06.26
 */
public class StationFareVO {

    @JsonProperty("OriginStationID")
    private String originStationId;

    @JsonProperty("DestinationStationID")
    private String destinationStationId;

    @JsonProperty("Fares")
    private List<FareDetail> fares;

    public String getOriginStationId() {
        return originStationId;
    }

    public String getDestinationStationId() {
        return destinationStationId;
    }

    public List<FareDetail> getFares() {
        return fares;
    }

    /**
     * 票價細節的靜態內部類別，解析 JSON 巢狀結構。
     * 處理一筆票價資料中，包含各種票價種類對應的價格
     */
    public static class FareDetail {

        @JsonProperty("FareClass")
        private Integer fareClass;

        @JsonProperty("CitizenCode")
        private String citizenCode;

        @JsonProperty("Price")
        private BigDecimal price;

        public Integer getFareClass() {
            return fareClass;
        }

        public String getCitizenCode() {
            return citizenCode;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }
}
