package com.mli.lookgo.module.metro.model.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳車站詳細資料的物件。
 *
 * @author D5042101
 * @since 2026.06.28
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "回傳車站詳細資料的物件")
public class StationDetailVO {

    @Schema(description = "車站 id", example = "1")
    private Integer id;

    @Schema(description = "車站中文名稱", example = "淡水")
    private String nameZhTw;

    @Schema(description = "車站英文名稱", example = "Tamsui")
    private String nameEn;

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

    @Schema(description = "電梯資訊", example = "出口1旁")
    private String elevator;

    @Schema(description = "電扶梯資訊", example = "出口1旁")
    private String escalator;

    @Schema(description = "更新時間 (UTC, ISO 8601)", example = "2026-06-25T12:00:00Z")
    private LocalDateTime updatedAt;

    public StationDetailVO() {
    }

    public StationDetailVO(Integer id, String nameZhTw, String nameEn,
            String atm, String nursingRoom, String diaperTable,
            String chargingStation, String ticketMachine, String locker,
            String drinkingWater, String restroom,
            String elevator, String escalator,
            LocalDateTime updatedAt) {
        this.id = id;
        this.nameZhTw = nameZhTw;
        this.nameEn = nameEn;
        this.atm = atm;
        this.nursingRoom = nursingRoom;
        this.diaperTable = diaperTable;
        this.chargingStation = chargingStation;
        this.ticketMachine = ticketMachine;
        this.locker = locker;
        this.drinkingWater = drinkingWater;
        this.restroom = restroom;
        this.elevator = elevator;
        this.escalator = escalator;
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

    @Override
    public String toString() {
        return "StationDetailVO{" +
                "id=" + id +
                ", nameZhTw='" + nameZhTw + '\'' +
                ", nameEn='" + nameEn + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
