package com.mli.lookgo.module.metro.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mli.lookgo.module.metro.model.entity.Line;
import com.mli.lookgo.module.metro.model.entity.Station;
import com.mli.lookgo.module.metro.service.MetroService;

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
}
