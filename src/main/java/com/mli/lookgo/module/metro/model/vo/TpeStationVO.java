package com.mli.lookgo.module.metro.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 對應 TPE 台北市開放資料 車站設施 API 回傳的 JSON 結構。
 *
 * @author D5042101
 * @since 2026.06.25
 */
public class TpeStationVO {

    @JsonProperty("車站名稱")
    private String stationName;

    @JsonProperty("銀行atm")
    private String atm;

    @JsonProperty("哺集乳室")
    private String nursingRoom;

    @JsonProperty("嬰兒尿布台")
    private String diaperTable;

    @JsonProperty("充電站")
    private String chargingStation;

    @JsonProperty("自動售票機")
    private String ticketMachine;

    @JsonProperty("飲水機/飲水壺")
    private String drinkingWater;

    @JsonProperty("廁所")
    private String restroom;

    public String getStationName() {
        return stationName;
    }

    public String getAtm() {
        return atm;
    }

    public String getNursingRoom() {
        return nursingRoom;
    }

    public String getDiaperTable() {
        return diaperTable;
    }

    public String getChargingStation() {
        return chargingStation;
    }

    public String getTicketMachine() {
        return ticketMachine;
    }

    public String getDrinkingWater() {
        return drinkingWater;
    }

    public String getRestroom() {
        return restroom;
    }
}
