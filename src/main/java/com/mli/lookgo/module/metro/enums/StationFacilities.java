package com.mli.lookgo.module.metro.enums;

/**
 * 車站設備相關的列舉，常數與 stations 資料表的設備欄位一一對應。
 *
 * @author D5042101
 * @since 2026.07.08
 */
public enum StationFacilities {

    ATM("ATM", "自動提款機"),
    NURSING_ROOM("Nursing Room", "哺乳室"),
    DIAPER_TABLE("Diaper Table", "尿布台"),
    CHARGING_STATION("Charging Station", "充電站"),
    TICKET_MACHINE("Ticket Machine", "自動售票機"),
    LOCKER("Locker", "置物櫃"),
    DRINKING_WATER("Drinking Water", "飲水機"),
    RESTROOM("Restroom", "廁所"),
    ELEVATOR("Elevator", "電梯"),
    ESCALATOR("Escalator", "電扶梯");

    private final String nameEn;
    private final String nameZhTw;

    StationFacilities(String nameEn, String nameZhTw) {
        this.nameEn = nameEn;
        this.nameZhTw = nameZhTw;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameZhTw() {
        return nameZhTw;
    }
}
