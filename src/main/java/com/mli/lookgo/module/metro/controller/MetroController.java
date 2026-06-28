package com.mli.lookgo.module.metro.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mli.lookgo.module.metro.model.dto.StationDetailDTO;
import com.mli.lookgo.module.metro.model.dto.TripPlanDTO;
import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.LineStation;
import com.mli.lookgo.module.metro.model.entity.LineTransfer;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.model.entity.StationFare;
import com.mli.lookgo.module.metro.model.vo.MapVO;
import com.mli.lookgo.module.metro.model.vo.StationDetailVO;
import com.mli.lookgo.module.metro.model.vo.TripPlanVO;
import com.mli.lookgo.module.metro.service.MetroService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 處理前端查詢捷運資料的 HTTP 請求的介面層。負責把資料傳給業務層處理，最後封裝結果為 HTTP 回應回傳給客戶端。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@RestController
@RequestMapping("/api/v1/metro")
@Tag(name = "Metro", description = "前端查詢捷運資料相關操作的 API")
public class MetroController {

    private final MetroService metroService;
    private static final Logger logger = LoggerFactory.getLogger(MetroController.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入查詢捷運資料相關的業務層 {@link MetroService}。
     *
     * @param metroService
     */
    public MetroController(MetroService metroService) {
        this.metroService = metroService;
    }

    /**
     * 取得所有路線資料。
     *
     * @return ResponseEntity<List<Line>>
     */
    @Operation(summary = "取得所有路線資料", description = "從資料庫取得所有路線資料")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得所有路線資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Line.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-all-line")
    public ResponseEntity<List<Line>> getAllLine() {
        logger.debug("收到查詢所有路線資料的請求");
        List<Line> lines = metroService.getAllLine();

        return ResponseEntity.ok(lines);
    }

    /**
     * 取得所有車站資料。
     *
     * @return ResponseEntity<List<Station>>
     */
    @Operation(summary = "取得所有車站資料", description = "從資料庫取得所有車站資料")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得所有車站資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Station.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-all-station")
    public ResponseEntity<List<Station>> getAllStation() {
        logger.debug("收到查詢所有車站資料的請求");
        List<Station> stations = metroService.getAllStation();

        return ResponseEntity.ok(stations);
    }

    /**
     * 取得所有路線車站資料。
     *
     * @return ResponseEntity<List<LineStation>>
     */
    @Operation(summary = "取得所有路線車站資料", description = "從資料庫取得所有路線車站資料")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得所有路線車站資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LineStation.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-all-line-station")
    public ResponseEntity<List<LineStation>> getAllLineStation() {
        logger.debug("收到查詢所有路線車站資料的請求");
        List<LineStation> lineStations = metroService.getAllLineStation();

        return ResponseEntity.ok(lineStations);
    }

    /**
     * 取得所有票價資料。
     *
     * @return ResponseEntity<List<StationFare>>
     */
    @Operation(summary = "取得所有票價資料", description = "從資料庫取得所有任意兩站間票價資料")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得所有票價資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StationFare.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-all-station-fare")
    public ResponseEntity<List<StationFare>> getAllStationFare() {
        logger.debug("收到查詢所有票價資料的請求");
        List<StationFare> stationFares = metroService.getAllStationFare();

        return ResponseEntity.ok(stationFares);
    }

    /**
     * 取得所有路線換乘資料。
     *
     * @return ResponseEntity<List<LineTransfer>>
     */
    @Operation(summary = "取得所有路線換乘資料", description = "從資料庫取得所有路線換乘站點與換乘時間資料")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得所有路線換乘資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LineTransfer.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-all-line-transfer")
    public ResponseEntity<List<LineTransfer>> getAllLineTransfer() {
        logger.debug("收到查詢所有路線換乘資料的請求");
        List<LineTransfer> lineTransfers = metroService.getAllLineTransfer();

        return ResponseEntity.ok(lineTransfers);
    }

    /**
     * 依車站代碼取得車站詳細資料。
     *
     * @param stationDetailDTO
     * @return ResponseEntity<StationDetailVO>
     */
    @Operation(summary = "依車站代碼取得車站詳細資料", description = "依傳入的車站代碼從資料庫取得該車站的詳細資料")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得車站詳細資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StationDetailVO.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入車站代碼!"))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-station-by-code")
    public ResponseEntity<StationDetailVO> getStationByCode(@Valid @RequestBody StationDetailDTO stationDetailDTO) {
        logger.debug("收到依車站代碼查詢車站詳細資料的請求，stationCode: {}", stationDetailDTO.getStationCode());
        StationDetailVO stationDetail = metroService.getStationByCode(stationDetailDTO);

        return ResponseEntity.ok(stationDetail);
    }

    /**
     * 取得捷運路網地圖資料（路線、車站順序、換乘連結），供前端 D3.js 繪製路線圖。
     *
     * @return ResponseEntity<MapVO>
     */
    @Operation(summary = "取得捷運路網地圖資料", description = "整合路線顏色、各路線有序車站清單與換乘連結，一次回傳供前端 D3.js 繪圖使用")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得捷運路網地圖資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MapVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-metro-map")
    public ResponseEntity<MapVO> getMetroMap() {
        logger.debug("收到查詢捷運路網地圖資料的請求");
        MapVO metroMap = metroService.getMetroMap();

        return ResponseEntity.ok(metroMap);
    }

    /**
     * 依起始、終點車站代碼計算行程路線規劃，可選擇性指定票種與路線策略。
     *
     * @param tripPlanDTO
     * @return ResponseEntity<TripPlanVO>
     */
    @Operation(summary = "計算捷運行程路線規劃", description = "依起終點車站代碼搜尋最佳路線，可選擇性指定票種（全票/優惠票等）與路線策略（最少轉乘次數/最短車程時間）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得行程路線規劃結果", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TripPlanVO.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入起始車站代碼!"))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-trip-plan")
    public ResponseEntity<TripPlanVO> getTripPlan(@Valid @RequestBody TripPlanDTO tripPlanDTO) {
        logger.debug("收到計算行程路線規劃的請求，fromStationCode: {}，toStationCode: {}",
                tripPlanDTO.getFromStationCode(), tripPlanDTO.getToStationCode());
        TripPlanVO tripPlan = metroService.getTripPlan(tripPlanDTO);

        return ResponseEntity.ok(tripPlan);
    }
}
