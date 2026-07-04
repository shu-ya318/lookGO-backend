package com.mli.lookgo.module.metro.model.vo;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 回傳起終點站詳細資料的物件。
 *
 * @author D5042101
 * @since 2026.06.28
 */
@Schema(description = "回傳起終點站詳細資料的物件")
public class OriginDestinationDetailVO {

    @Schema(description = "起始車站代碼", example = "R28")
    private String fromStationCode;

    @Schema(description = "終點車站代碼", example = "BL01")
    private String toStationCode;

    @Schema(description = "票種 (1=全票, 4=學生, 5=兒童, 7=愛心；未指定時為 null)", example = "1")
    private Integer fareType;

    @Schema(description = "路線策略 (1=最少轉乘次數, 2=最短車程時間)", example = "1")
    private Integer routingStrategy;

    @Schema(description = "行程路線段清單")
    private List<RouteSegmentVO> route;

    @Schema(description = "轉乘次數", example = "1")
    private int transferCount;

    @Schema(description = "總行駛時間 (秒)", example = "1800")
    private Integer totalTravelTimeSeconds;

    @Schema(description = "票價 (元；未指定票種時為 null)", example = "45.00")
    private BigDecimal farePrice;

    public OriginDestinationDetailVO() {
    }

    public OriginDestinationDetailVO(String fromStationCode, String toStationCode, Integer fareType,
            Integer routingStrategy, List<RouteSegmentVO> route, int transferCount,
            Integer totalTravelTimeSeconds, BigDecimal farePrice) {
        this.fromStationCode = fromStationCode;
        this.toStationCode = toStationCode;
        this.fareType = fareType;
        this.routingStrategy = routingStrategy;
        this.route = route;
        this.transferCount = transferCount;
        this.totalTravelTimeSeconds = totalTravelTimeSeconds;
        this.farePrice = farePrice;
    }

    public String getFromStationCode() {
        return fromStationCode;
    }

    public void setFromStationCode(String fromStationCode) {
        this.fromStationCode = fromStationCode;
    }

    public String getToStationCode() {
        return toStationCode;
    }

    public void setToStationCode(String toStationCode) {
        this.toStationCode = toStationCode;
    }

    public Integer getFareType() {
        return fareType;
    }

    public void setFareType(Integer fareType) {
        this.fareType = fareType;
    }

    public Integer getRoutingStrategy() {
        return routingStrategy;
    }

    public void setRoutingStrategy(Integer routingStrategy) {
        this.routingStrategy = routingStrategy;
    }

    public List<RouteSegmentVO> getRoute() {
        return route;
    }

    public void setRoute(List<RouteSegmentVO> route) {
        this.route = route;
    }

    public int getTransferCount() {
        return transferCount;
    }

    public void setTransferCount(int transferCount) {
        this.transferCount = transferCount;
    }

    public Integer getTotalTravelTimeSeconds() {
        return totalTravelTimeSeconds;
    }

    public void setTotalTravelTimeSeconds(Integer totalTravelTimeSeconds) {
        this.totalTravelTimeSeconds = totalTravelTimeSeconds;
    }

    public BigDecimal getFarePrice() {
        return farePrice;
    }

    public void setFarePrice(BigDecimal farePrice) {
        this.farePrice = farePrice;
    }

    @Override
    public String toString() {
        return "OriginDestinationDetailVO{" +
                "fromStationCode='" + fromStationCode + '\'' +
                ", toStationCode='" + toStationCode + '\'' +
                ", transferCount=" + transferCount +
                ", totalTravelTimeSeconds=" + totalTravelTimeSeconds +
                ", farePrice=" + farePrice +
                '}';
    }

    // ----- 路線段 -----

    @Schema(description = "單一路線段資訊（搭乘同一路線的連續車站）")
    public static class RouteSegmentVO {

        @Schema(description = "路線代號", example = "R")
        private String lineCode;

        @Schema(description = "路線中文名稱", example = "淡水信義線")
        private String lineNameZhTw;

        @Schema(description = "路線顏色 (Hex)", example = "#E3002C")
        private String lineColor;

        @Schema(description = "路線段內的車站清單（依行進方向排列）")
        private List<StationInfoVO> stations;

        @Schema(description = "本段行駛時間 (秒)", example = "720")
        private Integer segmentTimeSeconds;

        public RouteSegmentVO(String lineCode, String lineNameZhTw, String lineColor,
                List<StationInfoVO> stations, Integer segmentTimeSeconds) {
            this.lineCode = lineCode;
            this.lineNameZhTw = lineNameZhTw;
            this.lineColor = lineColor;
            this.stations = stations;
            this.segmentTimeSeconds = segmentTimeSeconds;
        }

        public String getLineCode() {
            return lineCode;
        }

        public String getLineNameZhTw() {
            return lineNameZhTw;
        }

        public String getLineColor() {
            return lineColor;
        }

        public List<StationInfoVO> getStations() {
            return stations;
        }

        public Integer getSegmentTimeSeconds() {
            return segmentTimeSeconds;
        }
    }

    // ----- 車站資訊 -----

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "路線段中的車站資訊")
    public static class StationInfoVO {

        @Schema(description = "車站代碼", example = "R10")
        private String stationCode;

        @Schema(description = "車站中文名稱", example = "台北車站")
        private String nameZhTw;

        @Schema(description = "車站英文名稱", example = "Taipei Main Station")
        private String nameEn;

        @Schema(description = "銀行 ATM 位置")
        private String atm;

        @Schema(description = "哺集乳室位置")
        private String nursingRoom;

        @Schema(description = "嬰兒尿布台位置")
        private String diaperTable;

        @Schema(description = "充電站位置")
        private String chargingStation;

        @Schema(description = "自動售票機位置")
        private String ticketMachine;

        @Schema(description = "置物櫃位置")
        private String locker;

        @Schema(description = "飲水機位置")
        private String drinkingWater;

        @Schema(description = "廁所位置")
        private String restroom;

        @Schema(description = "電梯資訊")
        private String elevator;

        @Schema(description = "電扶梯資訊")
        private String escalator;

        public StationInfoVO(String stationCode, String nameZhTw, String nameEn) {
            this.stationCode = stationCode;
            this.nameZhTw = nameZhTw;
            this.nameEn = nameEn;
        }

        public String getStationCode() {
            return stationCode;
        }

        public String getNameZhTw() {
            return nameZhTw;
        }

        public String getNameEn() {
            return nameEn;
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
    }
}
