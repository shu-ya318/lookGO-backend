package com.mli.lookgo.module.metro.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 對應 TDX Metro LineTransfer API 回傳的 JSON 結構。
 * 提供各路線間轉乘車站與轉乘所需時間，用於同步轉乘資料。
 *
 * @author D5042101
 * @since 2026.06.26
 */
public class LineTransferVO {

    @JsonProperty("FromLineID")
    private String fromLineId;

    @JsonProperty("FromStationID")
    private String fromStationId;

    @JsonProperty("ToLineID")
    private String toLineId;

    @JsonProperty("ToStationID")
    private String toStationId;

    @JsonProperty("TransferTime")
    private Integer transferTime;

    public String getFromLineId() {
        return fromLineId;
    }

    public String getFromStationId() {
        return fromStationId;
    }

    public String getToLineId() {
        return toLineId;
    }

    public String getToStationId() {
        return toStationId;
    }

    public Integer getTransferTime() {
        return transferTime;
    }
}
