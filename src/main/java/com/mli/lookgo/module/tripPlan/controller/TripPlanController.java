package com.mli.lookgo.module.tripPlan.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.core.result.PaginatedVO;
import com.mli.lookgo.module.tripPlan.model.dto.CreateTripPlanDTO;
import com.mli.lookgo.module.tripPlan.model.dto.TripPlanIdDTO;
import com.mli.lookgo.module.tripPlan.model.dto.UpdateTripPlanDTO;
import com.mli.lookgo.module.tripPlan.model.dto.UpdateTripPlanNameDTO;
import com.mli.lookgo.module.tripPlan.model.vo.TripPlanVO;
import com.mli.lookgo.module.tripPlan.service.TripPlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 處理使用者旅程規劃管理相關 HTTP 請求的介面層，僅限已登入使用者存取自己建立的旅程規劃。負責把資料傳給業務層處理，最後封裝結果為 HTTP
 * 回應回傳給客戶端。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@RestController
@RequestMapping("/api/v1/trip-plan")
@Tag(name = "Trip Plan", description = "使用者旅程規劃管理相關操作的 API")
public class TripPlanController {

        private static final Logger logger = LoggerFactory.getLogger(TripPlanController.class);

        private final TripPlanService tripPlanService;

        /**
         * 讓 Spring 容器能在應用程式啟動時，自動注入旅程規劃相關的業務層 {@link TripPlanService}。
         *
         * @param tripPlanService
         */
        public TripPlanController(TripPlanService tripPlanService) {
                this.tripPlanService = tripPlanService;
        }

        /**
         * 新增一筆旅程規劃。
         *
         * @param createTripPlanDTO
         * @return ResponseEntity<TripPlanVO>
         */
        @Operation(summary = "新增旅程規劃", description = "為當前使用者新增一筆旅程規劃")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "旅程規劃新增成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TripPlanVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤或已達旅程規劃數量上限", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "已達會員等級旅程規劃數量上限 (10 筆)，請先刪除部分旅程規劃!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到當前使用者或指定車站", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的車站!"))) })
        @PostMapping("/create-plan")
        public ResponseEntity<TripPlanVO> createTripPlan(@Valid @RequestBody CreateTripPlanDTO createTripPlanDTO) {
                logger.debug("收到新增旅程規劃的請求，createTripPlanDTO: {}", createTripPlanDTO);
                TripPlanVO tripPlan = tripPlanService.createTripPlan(createTripPlanDTO);

                return ResponseEntity.ok(tripPlan);
        }

        /**
         * 取得當前使用者分頁與模糊搜尋（旅程名稱）後的旅程規劃列表。
         *
         * @param keyword
         * @param page
         * @param size
         * @return ResponseEntity<PaginatedVO<TripPlanVO>>
         */
        @Operation(summary = "取得所有旅程規劃資料", description = "取得當前使用者建立的所有旅程規劃資料，支援分頁與模糊搜尋（比對旅程名稱）")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得旅程規劃資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedVO.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到當前使用者", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到當前使用者!"))) })
        @PostMapping("/get-all-plan-paginated")
        public ResponseEntity<PaginatedVO<TripPlanVO>> getAllTripPlan(
                        @Parameter(description = "搜尋關鍵字（旅程名稱）") @RequestParam(name = "keyword", required = false) String keyword,
                        @Parameter(description = "頁碼 (從 0 起算)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "8") int size) {
                logger.debug("收到分頁查詢旅程規劃資料的請求，keyword: {}, page: {}, size: {}", keyword, page, size);
                PaginatedVO<TripPlanVO> paginatedTripPlans = tripPlanService.getAllTripPlan(keyword, page, size);

                return ResponseEntity.ok(paginatedTripPlans);
        }

        /**
         * 取得當前使用者所有旅程規劃的名稱列表。
         *
         * @return ResponseEntity<List<String>>
         */
        @Operation(summary = "取得所有旅程規劃名稱", description = "取得當前使用者建立的所有旅程規劃名稱，依建立時間新到舊排序")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得旅程規劃名稱列表", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class, example = "淡水一日遊")))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到當前使用者", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到當前使用者!"))) })
        @PostMapping("/get-all-plan-name")
        public ResponseEntity<List<String>> getAllTripPlanName() {
                logger.debug("收到取得所有旅程規劃名稱的請求");
                List<String> tripPlanNames = tripPlanService.getAllTripPlanName();

                return ResponseEntity.ok(tripPlanNames);
        }

        /**
         * 取得單一旅程規劃資料，以旅程名稱模糊搜尋。
         *
         * @param keyword
         * @return ResponseEntity<TripPlanVO>
         */
        @Operation(summary = "取得單一旅程規劃資料", description = "以旅程名稱模糊搜尋當前使用者的旅程規劃，回傳符合條件中最新建立的一筆")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得旅程規劃資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TripPlanVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入旅程名稱!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到當前使用者或符合條件的旅程規劃", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到名稱包含「淡水一日遊」的旅程規劃!"))) })
        @PostMapping("/get-plan")
        public ResponseEntity<TripPlanVO> getTripPlan(
                        @Parameter(description = "搜尋關鍵字（旅程名稱，模糊比對）", required = true) @RequestParam(name = "keyword") String keyword) {
                logger.debug("收到以旅程名稱查詢單一旅程規劃的請求，keyword: {}", keyword);
                TripPlanVO tripPlan = tripPlanService.getTripPlanByName(keyword);

                return ResponseEntity.ok(tripPlan);
        }

        /**
         * 移除指定的旅程規劃。
         *
         * @param tripPlanIdDTO
         * @return ResponseEntity<MessageVO>
         */
        @Operation(summary = "移除旅程規劃", description = "軟刪除指定 id 的旅程規劃，僅本人可操作")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "旅程規劃刪除成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入旅程規劃id!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "非本人的旅程規劃", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "不得操作非本人的旅程規劃!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定旅程規劃", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的旅程規劃!"))) })
        @PostMapping("/delete-plan")
        public ResponseEntity<MessageVO> deleteTripPlan(@Valid @RequestBody TripPlanIdDTO tripPlanIdDTO) {
                logger.debug("收到刪除旅程規劃的請求，tripPlanIdDTO: {}", tripPlanIdDTO);
                MessageVO apiResult = tripPlanService.deleteTripPlan(tripPlanIdDTO.getTripPlanId());

                return ResponseEntity.ok(apiResult);
        }

        /**
         * 更新指定旅程規劃的名稱。
         *
         * @param updateTripPlanNameDTO
         * @return ResponseEntity<TripPlanVO>
         */
        @Operation(summary = "更新旅程規劃名稱", description = "更新指定 id 旅程規劃的名稱，僅本人可操作")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "旅程規劃名稱更新成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TripPlanVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入旅程名稱!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "非本人的旅程規劃", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "不得操作非本人的旅程規劃!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定旅程規劃", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的旅程規劃!"))) })
        @PostMapping("/update-plan-name")
        public ResponseEntity<TripPlanVO> updateTripPlanName(
                        @Valid @RequestBody UpdateTripPlanNameDTO updateTripPlanNameDTO) {
                logger.debug("收到更新旅程規劃名稱的請求，updateTripPlanNameDTO: {}", updateTripPlanNameDTO);
                TripPlanVO tripPlan = tripPlanService.updateTripPlanName(updateTripPlanNameDTO.getTripPlanId(),
                                updateTripPlanNameDTO.getName());

                return ResponseEntity.ok(tripPlan);
        }

        /**
         * 更新指定旅程規劃的資訊（起訖站以外）。
         *
         * @param updateTripPlanDTO
         * @return ResponseEntity<TripPlanVO>
         */
        @Operation(summary = "更新旅程規劃資訊", description = "更新指定 id 旅程規劃的票種、票價、轉乘次數、路線策略與備註（不含起訖站），僅本人可操作")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "旅程規劃資訊更新成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TripPlanVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "不支援的票種: 2，有效票種為 1(全票)、4(學生)、5(兒童)、7(愛心)"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "非本人的旅程規劃", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "不得操作非本人的旅程規劃!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定旅程規劃", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的旅程規劃!"))) })
        @PostMapping("/update-plan")
        public ResponseEntity<TripPlanVO> updateTripPlanInfo(@Valid @RequestBody UpdateTripPlanDTO updateTripPlanDTO) {
                logger.debug("收到更新旅程規劃資訊的請求，updateTripPlanDTO: {}", updateTripPlanDTO);
                TripPlanVO tripPlan = tripPlanService.updateTripPlanInfo(updateTripPlanDTO);

                return ResponseEntity.ok(tripPlan);
        }

        /**
         * 匯出指定旅程規劃的 excel 檔。
         *
         * @param tripPlanIdDTO
         * @return ResponseEntity<byte[]>
         */
        @Operation(summary = "匯出指定旅程規劃 excel", description = "取得指定 id 的旅程規劃並匯出 excel 檔，僅本人可操作")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功匯出旅程規劃 excel", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "非本人的旅程規劃", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "不得操作非本人的旅程規劃!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定旅程規劃", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的旅程規劃!"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "匯出旅程規劃 excel 報表發生錯誤!"))) })
        @PostMapping("/get-excel-by-trip-plan-id")
        public ResponseEntity<byte[]> getExcelByTripPlanId(@Valid @RequestBody TripPlanIdDTO tripPlanIdDTO) {
                logger.debug("收到依旅程規劃 id 匯出旅程規劃 excel 的請求，tripPlanIdDTO: {}", tripPlanIdDTO);
                byte[] excel = tripPlanService.exportTripPlanExcel(tripPlanIdDTO.getTripPlanId());

                String encodedFilename = UriUtils.encode("旅程規劃.xlsx", StandardCharsets.UTF_8);

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename*=utf-8''" + encodedFilename)
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(excel);
        }
}
