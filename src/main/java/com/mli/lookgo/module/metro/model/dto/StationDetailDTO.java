package com.mli.lookgo.module.metro.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 處理依車站代碼查詢車站詳細資料的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.28
 */
@Schema(description = "處理依車站代碼查詢車站詳細資料的資料傳輸物件")
public class StationDetailDTO {

    @Schema(description = "車站代碼", example = "BL01")
    @NotBlank(message = "請輸入車站代碼!")
    private String stationCode;

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    @Override
    public String toString() {
        return "StationDetailDTO{stationCode='" + stationCode + "'}";
    }
}
