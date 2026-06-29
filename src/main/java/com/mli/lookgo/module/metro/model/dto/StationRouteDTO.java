package com.mli.lookgo.module.metro.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 查詢起終點站詳細資料的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.28
 */
@Schema(description = "查詢起終點站詳細資料的資料傳輸物件")
public class StationRouteDTO {

    @Schema(description = "起始車站代碼", example = "R28")
    @NotBlank(message = "請輸入起始車站代碼!")
    private String fromStationCode;

    @Schema(description = "終點車站代碼", example = "BL12")
    @NotBlank(message = "請輸入終點車站代碼!")
    private String toStationCode;

    @Schema(description = "票種 (1=全票, 4=學生, 5=兒童, 7=愛心；未傳入時不計算票價)", example = "1")
    private Integer fareType;

    @Schema(description = "路線策略 (1=最少轉乘次數, 2=最短車程時間；未傳入時預設為 1)", example = "1")
    private Integer routingStrategy;

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

    @Override
    public String toString() {
        return "StationRouteDTO{fromStationCode='" + fromStationCode +
                "', toStationCode='" + toStationCode +
                "', fareType=" + fareType +
                ", routingStrategy=" + routingStrategy + '}';
    }
}
