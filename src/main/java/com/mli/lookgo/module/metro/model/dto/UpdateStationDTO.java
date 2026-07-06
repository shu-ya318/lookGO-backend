package com.mli.lookgo.module.metro.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 處理更新車站資料相關的資料傳輸物件。除 id 外其餘欄位皆為選填，僅會更新有帶值的欄位；
 * 車站中文名稱可於此修改，實際同步比對鍵為資料庫另一欄位 original_name_zh_tw，不受此處修改影響。
 *
 * @author D5042101
 * @since 2026.07.05
 */
@Schema(description = "處理更新車站資料相關的資料傳輸物件")
public class UpdateStationDTO {

    @Schema(description = "車站 id", example = "1")
    @NotNull(message = "請輸入車站id!")
    private Integer id;

    @Schema(description = "車站中文名稱", example = "淡水站")
    @Size(max = 100, message = "車站中文名稱長度不能超過 100 個字元!")
    private String nameZhTw;

    @Schema(description = "車站英文名稱", example = "Danshui")
    @Size(max = 200, message = "車站英文名稱長度不能超過 200 個字元!")
    private String nameEn;

    @Schema(description = "銀行 ATM 位置", example = "非付費區，近往出口3電梯")
    @Size(max = 1000, message = "銀行ATM位置長度不能超過 1000 個字元!")
    private String atm;

    @Schema(description = "哺集乳室位置", example = "付費區，B2大廳層")
    @Size(max = 1000, message = "哺集乳室位置長度不能超過 1000 個字元!")
    private String nursingRoom;

    @Schema(description = "嬰兒尿布台位置", example = "付費區，哺集乳室")
    @Size(max = 1000, message = "嬰兒尿布台位置長度不能超過 1000 個字元!")
    private String diaperTable;

    @Schema(description = "充電站位置", example = "非付費區，近往出口3電梯")
    @Size(max = 1000, message = "充電站位置長度不能超過 1000 個字元!")
    private String chargingStation;

    @Schema(description = "自動售票機位置", example = "近出口3")
    @Size(max = 1000, message = "自動售票機位置長度不能超過 1000 個字元!")
    private String ticketMachine;

    @Schema(description = "置物櫃位置")
    @Size(max = 1000, message = "置物櫃位置長度不能超過 1000 個字元!")
    private String locker;

    @Schema(description = "飲水機位置", example = "出口3")
    @Size(max = 1000, message = "飲水機位置長度不能超過 1000 個字元!")
    private String drinkingWater;

    @Schema(description = "廁所位置", example = "非付費區，近出口3")
    @Size(max = 1000, message = "廁所位置長度不能超過 1000 個字元!")
    private String restroom;

    @Schema(description = "電梯資訊", example = "出口1旁")
    @Size(max = 1000, message = "電梯資訊長度不能超過 1000 個字元!")
    private String elevator;

    @Schema(description = "電扶梯資訊", example = "出口1旁")
    @Size(max = 1000, message = "電扶梯資訊長度不能超過 1000 個字元!")
    private String escalator;

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

    /**
     * 判斷除 id 外是否所有欄位皆未帶值，供業務層擋下無實質異動的更新請求。
     *
     * @return 除 id 外所有欄位皆為 null 時回傳 true，否則 false
     */
    public boolean hasNoUpdatableField() {
        return nameZhTw == null && nameEn == null && atm == null && nursingRoom == null && diaperTable == null
                && chargingStation == null && ticketMachine == null && locker == null
                && drinkingWater == null && restroom == null && elevator == null && escalator == null;
    }

    @Override
    public String toString() {
        return "UpdateStationDTO{id=" + id + ", nameZhTw='" + nameZhTw + '\'' + ", nameEn='" + nameEn + '\'' + '}';
    }
}
