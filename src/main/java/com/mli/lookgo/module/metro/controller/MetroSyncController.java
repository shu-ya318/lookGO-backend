package com.mli.lookgo.module.metro.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.module.metro.model.vo.SyncStatusVO;
import com.mli.lookgo.module.metro.service.MetroSyncService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 處理從外部 API 同步捷運資料的 HTTP 請求的介面層。負責觸發同步作業，最後封裝結果為 HTTP 回應回傳給客戶端。
 *
 * @author D5042101
 * @since 2026.06.24
 */
@RestController
@RequestMapping("/api/v1/metro/sync")
@Tag(name = "Metro Sync", description = "從外部第三方 API 同步捷運資料的操作")
public class MetroSyncController {

    private final MetroSyncService metroSyncService;

    private static final Logger logger = LoggerFactory.getLogger(MetroSyncController.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入同步捷運資料相關的業務層 {@link MetroSyncService}。
     *
     * @param metroSyncService
     */
    public MetroSyncController(MetroSyncService metroSyncService) {
        this.metroSyncService = metroSyncService;
    }

    /**
     * 從 TDX API 同步路線資料到資料庫，僅限 ADMIN 角色存取。
     *
     * @return ResponseEntity<MessageVO>
     */
    @Operation(summary = "同步路線資料", description = "從 TDX API 同步路線資料到資料庫，僅限 ADMIN 角色存取")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "路線資料同步成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-line")
    public ResponseEntity<MessageVO> syncAllLine() {
        logger.debug("收到同步路線資料的請求");
        MessageVO apiResult = metroSyncService.syncAllLine();

        return ResponseEntity.ok(apiResult);
    }

    /**
     * 從 TDX + DataTaipei API 同步車站資料到資料庫，僅限 ADMIN 角色存取。
     *
     * @return ResponseEntity<MessageVO>
     */
    @Operation(summary = "同步車站資料", description = "從 TDX API 取得車站名稱，從 DataTaipei API取得車站設施，合併後同步到資料庫。僅限 ADMIN 角色存取")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "車站資料同步成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-station")
    public ResponseEntity<MessageVO> syncAllStation() {
        logger.debug("收到同步車站資料的請求");
        MessageVO apiResult = metroSyncService.syncAllStation();

        return ResponseEntity.ok(apiResult);
    }

    /**
     * 從 TDX API 同步路線車站資料到資料庫，僅限 ADMIN 角色存取。
     *
     * @return ResponseEntity<MessageVO>
     */
    @Operation(summary = "同步路線車站資料", description = "從 TDX API 同步路線車站資料到資料庫，需先同步路線和車站資料。僅限 ADMIN 角色存取")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "路線車站資料同步成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-line-station")
    public ResponseEntity<MessageVO> syncAllLineStation() {
        logger.debug("收到同步路線車站資料的請求");
        MessageVO apiResult = metroSyncService.syncAllLineStation();

        return ResponseEntity.ok(apiResult);
    }

    /**
     * 從 TDX S2STravelTime API 同步路線車站累計行駛時間到資料庫，僅限 ADMIN 角色存取。
     *
     * @return ResponseEntity<MessageVO>
     */
    @Operation(summary = "同步路線車站累計行駛時間", description = "從 TDX S2STravelTime API 計算各站累計行駛時間並更新資料庫，需先同步路線車站資料。僅限 ADMIN 角色存取")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "累計行駛時間同步成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-line-station-cumulative-time")
    public ResponseEntity<MessageVO> syncAllLineStationCumulativeTime() {
        logger.debug("收到同步路線車站累計行駛時間的請求");
        MessageVO apiResult = metroSyncService.syncAllLineStationCumulativeTime();

        return ResponseEntity.ok(apiResult);
    }

    /**
     * 觸發背景同步票價資料，立即回 202 Accepted，僅限 ADMIN 角色存取。
     * 因 TDX 票價 API 資料量大、同步時間可長達數分鐘，改為背景執行；
     * 前端透過 {@link #getStationFareSyncStatus()} 輪詢追蹤進度。
     *
     * @return ResponseEntity<MessageVO>
     */
    @Operation(summary = "觸發背景同步票價資料", description = "觸發背景同步票價資料並立即回 202；需先同步路線車站資料，透過狀態查詢 API 追蹤進度。僅限 ADMIN 角色存取")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "已開始背景同步票價資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
            @ApiResponse(responseCode = "409", description = "票價同步正在進行中", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class, example = "票價同步正在進行中，請勿重複觸發!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-station-fare")
    public ResponseEntity<MessageVO> syncAllStationFare() {
        logger.debug("收到觸發背景同步票價資料的請求");
        metroSyncService.startSyncAllStationFare();

        return ResponseEntity.accepted()
                .body(new MessageVO("已開始背景同步票價資料，請透過狀態查詢 API 追蹤進度!"));
    }

    /**
     * 查詢票價背景同步的當前狀態與進度，僅限 ADMIN 角色存取。
     * 註：狀態為 in-memory 儲存，伺服器重啟後回到 IDLE（upsert 冪等，重按一次即可）。
     *
     * @return ResponseEntity<SyncStatusVO>
     */
    @Operation(summary = "查詢票價同步狀態", description = "查詢票價背景同步的當前狀態、進度百分比與階段訊息，供前端輪詢。狀態為 in-memory 儲存，伺服器重啟後回到 IDLE。僅限 ADMIN 角色存取")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查詢成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SyncStatusVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-station-fare/status")
    public ResponseEntity<SyncStatusVO> getStationFareSyncStatus() {
        logger.debug("收到查詢票價同步狀態的請求");
        SyncStatusVO apiResult = metroSyncService.getStationFareSyncStatus();

        return ResponseEntity.ok(apiResult);
    }

    /**
     * 從 TDX LineTransfer API 同步路線換乘資料到資料庫，僅限 ADMIN 角色存取。
     *
     * @return ResponseEntity<MessageVO>
     */
    @Operation(summary = "同步路線換乘資料", description = "從 TDX LineTransfer API 同步各路線間換乘車站與換乘時間到資料庫，需先同步路線車站資料。僅限 ADMIN 角色存取")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "路線換乘資料同步成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
            @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
            @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync-all-line-transfer")
    public ResponseEntity<MessageVO> syncAllLineTransfer() {
        logger.debug("收到同步路線換乘資料的請求");
        MessageVO apiResult = metroSyncService.syncAllLineTransfer();

        return ResponseEntity.ok(apiResult);
    }
}
