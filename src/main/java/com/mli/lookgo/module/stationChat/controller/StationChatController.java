package com.mli.lookgo.module.stationChat.controller;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.core.result.PaginatedVO;
import com.mli.lookgo.module.stationChat.model.dto.CreateAnnouncementDTO;
import com.mli.lookgo.module.stationChat.model.dto.DeleteAnnouncementDTO;
import com.mli.lookgo.module.stationChat.model.dto.StationIdDTO;
import com.mli.lookgo.module.stationChat.model.dto.UpdateAnnouncementDTO;
import com.mli.lookgo.module.stationChat.model.vo.StationChatAnnouncementVO;
import com.mli.lookgo.module.stationChat.model.vo.StationChatMessageVO;
import com.mli.lookgo.module.stationChat.model.vo.UpdateAnnouncementVO;
import com.mli.lookgo.module.stationChat.service.StationChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 處理車站聊天留言與公告相關 HTTP 請求。
 *
 * @author D5042101
 * @since 2026.07.03
 */
@RestController
@RequestMapping("/api/v1/station-chat")
@Tag(name = "Station Chat", description = "車站聊天室相關操作的 API")
public class StationChatController {

        private final StationChatService stationChatService;

        private static final Logger logger = LoggerFactory.getLogger(StationChatController.class);

        /**
         * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
         *
         * @param stationChatService
         */
        public StationChatController(StationChatService stationChatService) {
                this.stationChatService = stationChatService;
        }

        /**
         * 依車站 id 分頁取得該車站的歷史聊天留言。
         *
         * @param stationId
         * @param page
         * @param size
         * @return ResponseEntity<PaginatedVO<StationChatMessageVO>>
         */
        @Operation(summary = "依車站 id 分頁取得車站聊天留言", description = "取得指定車站的歷史聊天留言，支援分頁，依建立時間新到舊排序")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得車站聊天留言", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedVO.class))),
                        @ApiResponse(responseCode = "401", description = "Token 無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定車站", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的車站!"))) })
        @PostMapping("/get-message-by-station-id")
        public ResponseEntity<PaginatedVO<StationChatMessageVO>> getMessagesByStationId(
                        @Parameter(description = "車站 id") @RequestParam Integer stationId,
                        @Parameter(description = "頁碼 (從 0 起算)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "8") int size) {
                logger.debug("收到依車站 id 分頁查詢車站聊天留言的請求，stationId: {}, page: {}, size: {}", stationId, page, size);
                PaginatedVO<StationChatMessageVO> paginatedMessages = stationChatService.getMessages(stationId, page,
                                size);

                return ResponseEntity.ok(paginatedMessages);
        }

        /**
         * 依車站 id 分頁取得該車站的公告列表。
         *
         * @param stationId
         * @param page
         * @param size
         * @return ResponseEntity<PaginatedVO<StationChatAnnouncementVO>>
         */
        @Operation(summary = "依車站 id 分頁取得車站聊天公告", description = "取得指定車站的公告，支援分頁，依建立時間新到舊排序")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功取得車站聊天公告", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedVO.class))),
                        @ApiResponse(responseCode = "401", description = "Token 無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定車站", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的車站!"))) })
        @PostMapping("/get-announcement-by-station-id")
        public ResponseEntity<PaginatedVO<StationChatAnnouncementVO>> getAnnouncementsByStationId(
                        @Parameter(description = "車站 id") @RequestParam Integer stationId,
                        @Parameter(description = "頁碼 (從 0 起算)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "8") int size) {
                logger.debug("收到依車站 id 分頁查詢車站聊天公告的請求，stationId: {}, page: {}, size: {}", stationId, page, size);
                PaginatedVO<StationChatAnnouncementVO> paginatedAnnouncements = stationChatService
                                .getAnnouncements(stationId, page, size);

                return ResponseEntity.ok(paginatedAnnouncements);
        }

        /**
         * 依車站 id 匯出該車站完整的聊天紀錄 excel 檔。
         *
         * @param stationIdDTO
         * @return ResponseEntity<byte[]>
         */
        @Operation(summary = "依車站 id 匯出車站完整聊天紀錄 excel", description = "取得指定車站的完整（不分頁）聊天紀錄並匯出 excel 檔")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "成功匯出車站當日聊天紀錄 excel", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
                        @ApiResponse(responseCode = "401", description = "Token 無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定車站", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的車站!"))) })
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/get-excel-by-station-id")
        public ResponseEntity<byte[]> getExcelByStationId(@Valid @RequestBody StationIdDTO stationIdDTO) {
                logger.debug("收到依車站 id 匯出車站當日聊天紀錄 excel 的請求，stationIdDTO: {}", stationIdDTO);
                byte[] excel = stationChatService.exportMessagesByStationId(stationIdDTO.getStationId());

                String encodedFilename = UriUtils.encode("車站當日聊天紀錄.xlsx", StandardCharsets.UTF_8);

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename*=utf-8''" + encodedFilename)
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(excel);
        }

        /**
         * 新增一筆車站聊天公告。
         *
         * @param createAnnouncementDTO
         * @return ResponseEntity<MessageVO>
         */
        @Operation(summary = "新增車站聊天公告", description = "於指定車站新增一筆公告")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "公告新增成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
                        @ApiResponse(responseCode = "401", description = "Token 無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
                        @ApiResponse(responseCode = "404", description = "找不到當前使用者或指定車站", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的車站!"))) })
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/create-announcement")
        public ResponseEntity<MessageVO> createAnnouncement(
                        @Valid @RequestBody CreateAnnouncementDTO createAnnouncementDTO) {
                logger.debug("收到新增車站聊天公告的請求，createAnnouncementDTO: {}", createAnnouncementDTO);
                MessageVO apiResult = stationChatService.createAnnouncement(createAnnouncementDTO);

                return ResponseEntity.ok(apiResult);
        }

        /**
         * 編輯指定公告的內容。
         *
         * @param updateAnnouncementDTO
         * @return ResponseEntity<UpdateAnnouncementVO>
         */
        @Operation(summary = "編輯車站聊天公告", description = "更新指定公告 id 的內容")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "公告編輯成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UpdateAnnouncementVO.class))),
                        @ApiResponse(responseCode = "401", description = "Token 無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定公告", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的公告!"))) })
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/update-announcement")
        public ResponseEntity<UpdateAnnouncementVO> updateAnnouncement(
                        @Valid @RequestBody UpdateAnnouncementDTO updateAnnouncementDTO) {
                logger.debug("收到編輯車站聊天公告的請求，updateAnnouncementDTO: {}", updateAnnouncementDTO);
                UpdateAnnouncementVO apiResult = stationChatService.updateAnnouncement(updateAnnouncementDTO);

                return ResponseEntity.ok(apiResult);
        }

        /**
         * 軟刪除指定公告。
         *
         * @param deleteAnnouncementDTO
         * @return ResponseEntity<MessageVO>
         */
        @Operation(summary = "刪除車站聊天公告", description = "軟刪除指定公告 id 的公告")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "公告刪除成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
                        @ApiResponse(responseCode = "401", description = "Token 無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
                        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
                        @ApiResponse(responseCode = "404", description = "找不到指定公告", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到 id:1 的公告!"))) })
        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/delete-announcement")
        public ResponseEntity<MessageVO> deleteAnnouncement(
                        @Valid @RequestBody DeleteAnnouncementDTO deleteAnnouncementDTO) {
                logger.debug("收到刪除車站聊天公告的請求，deleteAnnouncementDTO: {}", deleteAnnouncementDTO);
                MessageVO apiResult = stationChatService.deleteAnnouncement(deleteAnnouncementDTO.getAnnouncementId());

                return ResponseEntity.ok(apiResult);
        }
}
