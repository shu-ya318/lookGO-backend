package com.mli.lookgo.module.metro.model.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理捷運車站相關的資料。
 *
 * @author D5042101
 * @since 2026.06.25
 */
@Schema(description = "處理捷運車站相關的資料")
public class Station {

    @Schema(description = "車站 id", example = "1")
    private Integer id;

    @Schema(description = "車站中文名稱", example = "松山機場")
    private String nameZhTw;

    @Schema(description = "車站英文名稱", example = "Songshan Airport")
    private String nameEn;

    @Schema(description = "狀態 (0=停用, 1=啟用)", example = "1")
    private Integer status;

    @Schema(description = "銀行 ATM 位置", example = "非付費區，近往出口3電梯")
    private String atm;

    @Schema(description = "哺集乳室位置", example = "付費區，B2大廳層")
    private String nursingRoom;

    @Schema(description = "嬰兒尿布台位置", example = "付費區，哺集乳室")
    private String diaperTable;

    @Schema(description = "充電站位置", example = "非付費區，近往出口3電梯")
    private String chargingStation;

    @Schema(description = "自動售票機位置", example = "近出口3")
    private String ticketMachine;

    @Schema(description = "置物櫃位置")
    private String locker;

    @Schema(description = "飲水機位置", example = "出口3")
    private String drinkingWater;

    @Schema(description = "廁所位置", example = "非付費區，近出口3")
    private String restroom;

    @Schema(description = "建立時間 (UTC, ISO 8601)", example = "2026-06-25T12:00:00Z")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-06-25T12:00:00Z")
    private LocalDateTime updatedAt;

    public Station() {
    }

    public Station(String nameZhTw, String nameEn, Integer status,
            String atm, String nursingRoom, String diaperTable,
            String chargingStation, String ticketMachine,
            String drinkingWater, String restroom,
            LocalDateTime updatedAt) {
        this.nameZhTw = nameZhTw;
        this.nameEn = nameEn;
        this.status = status;
        this.atm = atm;
        this.nursingRoom = nursingRoom;
        this.diaperTable = diaperTable;
        this.chargingStation = chargingStation;
        this.ticketMachine = ticketMachine;
        this.drinkingWater = drinkingWater;
        this.restroom = restroom;
        this.updatedAt = updatedAt;
    }

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Station{" +
                "id=" + id +
                ", nameZhTw='" + nameZhTw + '\'' +
                ", nameEn='" + nameEn + '\'' +
                ", status=" + status +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
