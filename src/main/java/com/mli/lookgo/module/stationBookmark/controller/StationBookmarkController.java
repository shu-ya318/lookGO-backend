package com.mli.lookgo.module.stationBookmark.controller;

import java.nio.charset.StandardCharsets;

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
import com.mli.lookgo.module.stationBookmark.model.dto.BookmarkIdDTO;
import com.mli.lookgo.module.stationBookmark.model.dto.CreateBookmarkDTO;
import com.mli.lookgo.module.stationBookmark.model.vo.StationBookmarkVO;
import com.mli.lookgo.module.stationBookmark.service.StationBookmarkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 處理車站書籤管理相關 HTTP 請求的介面層。
 * 負責把資料傳給業務層處理，最後封裝結果為 HTTP 回應回傳給客戶端。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@RestController
@RequestMapping("/api/v1/station-bookmark")
@Tag(name = "Station Bookmark", description = "車站書籤管理相關操作的 API")
public class StationBookmarkController {

        private static final Logger logger = LoggerFactory.getLogger(StationBookmarkController.class);

        private final StationBookmarkService stationBookmarkService;

        /**
         * 讓 Spring 容器能在應用程式啟動時，自動注入車站書籤相關的業務層 {@link StationBookmarkService}。
         *
         * @param stationBookmarkService
         */
        public StationBookmarkController(StationBookmarkService stationBookmarkService) {
                this.stationBookmarkService = stationBookmarkService;
        }

        /**
         * 為當前使用者新增一筆車站書籤。
         *
         * @param createBookmarkDTO
         * @return ResponseEntity<StationBookmarkVO>
         */
        @Operation(summary = "新增車站書籤", description = "為當前使用者收藏指定車站")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "書籤新增成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StationBookmarkVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤、重複收藏或已達書籤數量上限", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "已對 id:3 的車站建立過書籤!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到當前使用者或指定車站", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的車站!"))) })
        @PostMapping("/create-bookmark")
        public ResponseEntity<StationBookmarkVO> createBookmark(
                        @Valid @RequestBody CreateBookmarkDTO createBookmarkDTO) {
                logger.debug("收到新增車站書籤的請求，createBookmarkDTO: {}", createBookmarkDTO);
                StationBookmarkVO bookmark = stationBookmarkService.createBookmark(createBookmarkDTO);

                return ResponseEntity.ok(bookmark);
        }

        /**
         * 依車站中文名稱模糊搜尋，取得當前使用者的單一車站書籤。
         *
         * @param stationName
         * @return ResponseEntity<StationBookmarkVO>
         */
        @Operation(summary = "取得單一車站書籤", description = "依車站中文名稱模糊搜尋，取得當前使用者收藏的單一車站書籤，若比對到多筆則取收藏時間最新的一筆")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得車站書籤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StationBookmarkVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "缺少必要的請求參數: stationName"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到當前使用者或符合條件的車站書籤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到符合「淡水」的車站書籤!"))) })
        @PostMapping("/get-bookmark-by-station-name")
        public ResponseEntity<StationBookmarkVO> getBookmarkByStationName(
                        @Parameter(description = "車站中文名稱關鍵字", required = true) @RequestParam(name = "stationName") String stationName) {
                logger.debug("收到取得單一車站書籤的請求，stationName: {}", stationName);
                StationBookmarkVO bookmark = stationBookmarkService.getBookmarkByStationName(stationName);

                return ResponseEntity.ok(bookmark);
        }

        /**
         * 取得當前使用者分頁與模糊搜尋後的車站書籤列表。
         *
         * @param keyword
         * @param page
         * @param size
         * @param sortDirection
         * @return ResponseEntity<PaginatedVO<StationBookmarkVO>>
         */
        @Operation(summary = "取得所有車站書籤資料", description = "取得當前使用者收藏的所有車站書籤資料，支援分頁與模糊搜尋（比對車站名稱、使用者名稱、email），並可依收藏時間排序")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得所有車站書籤資料", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "不支援的排序方向: XXX，有效值為 ASC、DESC"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到當前使用者", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到當前使用者!"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-all-bookmark-paginated")
        public ResponseEntity<PaginatedVO<StationBookmarkVO>> getAllBookmark(
                        @Parameter(description = "搜尋關鍵字") @RequestParam(name = "keyword", required = false) String keyword,
                        @Parameter(description = "頁碼 (從 0 起算)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "8") int size,
                        @Parameter(description = "排序方向（排序鍵為收藏時間）：DESC=新到舊（預設）、ASC=舊到新") @RequestParam(defaultValue = "DESC") String sortDirection) {
                logger.debug("收到分頁查詢車站書籤資料的請求，keyword: {}, page: {}, size: {}, sortDirection: {}", keyword, page, size,
                                sortDirection);
                PaginatedVO<StationBookmarkVO> paginatedBookmarks = stationBookmarkService.getAllBookmark(keyword, page,
                                size, sortDirection);

                return ResponseEntity.ok(paginatedBookmarks);
        }

        /**
         * 移除指定的車站書籤。
         *
         * @param bookmarkIdDTO
         * @return ResponseEntity<MessageVO>
         */
        @Operation(summary = "移除車站書籤", description = "軟刪除指定 id 的車站書籤")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "書籤刪除成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
                        @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入書籤id!"))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定書籤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的車站書籤!"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/delete-bookmark")
        public ResponseEntity<MessageVO> deleteBookmark(@Valid @RequestBody BookmarkIdDTO bookmarkIdDTO) {
                logger.debug("收到刪除車站書籤的請求，bookmarkIdDTO: {}", bookmarkIdDTO);
                MessageVO apiResult = stationBookmarkService.deleteBookmark(bookmarkIdDTO.getBookmarkId());

                return ResponseEntity.ok(apiResult);
        }

        /**
         * 匯出當前使用者所有有效（未軟刪除）的車站書籤 excel 檔。
         *
         * @return ResponseEntity<byte[]>
         */
        @Operation(summary = "匯出所有車站書籤 excel", description = "取得當前使用者所有有效（未軟刪除）的車站書籤並匯出 excel 檔，不受列表分頁與關鍵字篩選影響")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功匯出車站書籤 excel", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
                        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到當前使用者", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到當前使用者!"))),
                        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
        @PostMapping("/get-excel")
        public ResponseEntity<byte[]> getExcel() {
                logger.debug("收到匯出車站書籤 excel 的請求");
                byte[] excel = stationBookmarkService.exportBookmarkExcel();

                String encodedFilename = UriUtils.encode("車站書籤清單.xlsx", StandardCharsets.UTF_8);

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename*=utf-8''" + encodedFilename)
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(excel);
        }
}
