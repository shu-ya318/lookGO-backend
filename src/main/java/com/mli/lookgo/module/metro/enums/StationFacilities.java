package com.mli.lookgo.module.metro.enums;

public enum StationFacilities {

    TOILET("Toilet", "廁所"),
    ELEVATOR("Elevator", "電梯"),
    ACCESSIBLE_FACILITIES("Accessible Facilities", "無障礙設施"),
    NURSING_ROOM("Nursing Room", "哺乳室"),
    ATM("ATM", "ATM"),
    LOCKER("Locker", "置物櫃"),
    CHARGING_STATION("Charging Station", "充電站"),
    TICKET_MACHINE("Ticket Machine", "自動售票機"),
    DRINKING_WATER("Drinking Water", "飲水機");

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
