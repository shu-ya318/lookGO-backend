package com.mli.lookgo.module.metro.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳車站資料更新結果的物件，僅包含本次請求有帶值（實際異動）的欄位，其餘欄位為 null。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳車站資料更新結果的物件，僅包含本次實際異動的欄位")
public class UpdateStationVO {

    @Schema(description = "車站 id", example = "1")
    private Integer id;

    @Schema(description = "車站中文名稱", example = "淡水站", nullable = true)
    private String nameZhTw;

    @Schema(description = "車站英文名稱", example = "Danshui", nullable = true)
    private String nameEn;

    @Schema(description = "銀行 ATM 位置", nullable = true)
    private String atm;

    @Schema(description = "哺集乳室位置", nullable = true)
    private String nursingRoom;

    @Schema(description = "嬰兒尿布台位置", nullable = true)
    private String diaperTable;

    @Schema(description = "充電站位置", nullable = true)
    private String chargingStation;

    @Schema(description = "自動售票機位置", nullable = true)
    private String ticketMachine;

    @Schema(description = "置物櫃位置", nullable = true)
    private String locker;

    @Schema(description = "飲水機位置", nullable = true)
    private String drinkingWater;

    @Schema(description = "廁所位置", nullable = true)
    private String restroom;

    @Schema(description = "電梯資訊", nullable = true)
    private String elevator;

    @Schema(description = "電扶梯資訊", nullable = true)
    private String escalator;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-07-14T08:00:00Z")
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNameZhTw() {
        return nameZhTw;
    }

    public void setNameZhTw(String nameZhTw) {
        this.nameZhTw = nameZhTw;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getAtm() {
        return atm;
    }

    public void setAtm(String atm) {
        this.atm = atm;
    }

    public String getNursingRoom() {
        return nursingRoom;
    }

    public void setNursingRoom(String nursingRoom) {
        this.nursingRoom = nursingRoom;
    }

    public String getDiaperTable() {
        return diaperTable;
    }

    public void setDiaperTable(String diaperTable) {
        this.diaperTable = diaperTable;
    }

    public String getChargingStation() {
        return chargingStation;
    }

    public void setChargingStation(String chargingStation) {
        this.chargingStation = chargingStation;
    }

    public String getTicketMachine() {
        return ticketMachine;
    }

    public void setTicketMachine(String ticketMachine) {
        this.ticketMachine = ticketMachine;
    }

    public String getLocker() {
        return locker;
    }

    public void setLocker(String locker) {
        this.locker = locker;
    }

    public String getDrinkingWater() {
        return drinkingWater;
    }

    public void setDrinkingWater(String drinkingWater) {
        this.drinkingWater = drinkingWater;
    }

    public String getRestroom() {
        return restroom;
    }

    public void setRestroom(String restroom) {
        this.restroom = restroom;
    }

    public String getElevator() {
        return elevator;
    }

    public void setElevator(String elevator) {
        this.elevator = elevator;
    }

    public String getEscalator() {
        return escalator;
    }

    public void setEscalator(String escalator) {
        this.escalator = escalator;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
