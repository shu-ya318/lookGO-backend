package com.mli.lookgo.module.metro.model.dto;

import java.util.List;

import com.mli.lookgo.module.metro.enums.StationFacilities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 處理依車站代碼查詢車站詳細資料的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.28
 */
@Schema(description = "處理依車站代碼查詢車站詳細資料的資料傳輸物件")
public class StationDetailsDTO {

    @Schema(description = "車站代碼", example = "BL01")
    @NotBlank(message = "請輸入車站代碼!")
    private String stationCode;

    @Schema(description = "指定的車站設備過濾清單，例如: ATM, RESTROOM, ELEVATOR")
    private List<StationFacilities> stationFacilities;

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public List<StationFacilities> getStationFacilities() {
        return stationFacilities;
    }

    public void setStationFacilities(List<StationFacilities> stationFacilities) {
        this.stationFacilities = stationFacilities;
    }

    @Override
    public String toString() {
        return "StationDetailsDTO{stationCode='" + stationCode + "', stationFacilities=" + stationFacilities + "}";
    }
}
