package com.mli.lookgo.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.module.metro.exceptions.StationNotFoundException;
import com.mli.lookgo.module.metro.exceptions.SyncInProgressException;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkDuplicateException;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkLimitExceededException;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkNotFoundException;
import com.mli.lookgo.module.stationBookmark.exceptions.StationBookmarkExportExcelFailedException;
import com.mli.lookgo.module.stationChat.exceptions.InvalidChatContentException;
import com.mli.lookgo.module.stationChat.exceptions.StationChatExportExcelFailedException;
import com.mli.lookgo.module.stationChat.exceptions.StationChatNotFoundException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanAccessDeniedException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanExportExcelFailedException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanLimitExceededException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanNameDuplicationException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanNotFoundException;
import com.mli.lookgo.module.user.exceptions.AdminStatusModificationException;
import com.mli.lookgo.module.user.exceptions.InvalidAvatarException;
import com.mli.lookgo.module.user.exceptions.UserNotFoundException;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 以全域方式捕捉所有拋出的例外並進行對應處理。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 處理方法參數無效相關的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        logger.error("發生方法參數無效例外的錯誤: {}", exception.getMessage());

        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * 處理非法參數相關的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageVO> handleIllegalArgumentException(IllegalArgumentException exception) {
        logger.error("發生非法請求參數例外的錯誤: {}", exception.getMessage());

        MessageVO apiErrorResponse = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiErrorResponse);
    }

    /**
     * 處理執行時期相關的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 500 (Internal Server Error) 給客戶端。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageVO> handleRuntimeException(RuntimeException exception) {
        logger.error("發生執行時期例外的錯誤: {}", exception.getMessage());

        MessageVO apiErrorResponse = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiErrorResponse);
    }

    // ----- Core (Auth) -----

    /**
     * 處理請求缺少必要 Query Parameter 的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<MessageVO> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception) {
        logger.error("缺少必要的請求參數: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO("缺少必要的請求參數: " + exception.getParameterName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResult);
    }

    /**
     * 處理存取被拒絕的例外（權限不足）。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 403 (Forbidden) 給客戶端。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageVO> handleAccessDeniedException(AccessDeniedException exception) {
        logger.error("存取被拒絕: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO("權限不足，無法操作!");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResult);
    }

    /**
     * 處理忘記密碼驗證失敗的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(ForgetPasswordVerificationException.class)
    public ResponseEntity<MessageVO> handleForgetPasswordVerificationException(
            ForgetPasswordVerificationException exception) {
        logger.error("忘記密碼驗證失敗: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResult);
    }

    // ----- User -----

    /**
     * 處理使用者重複建立的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(UserDuplicateException.class)
    public ResponseEntity<MessageVO> handleUserDuplicateException(UserDuplicateException exception) {
        logger.error("不能重複建立: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResult);
    }

    /**
     * 處理找不到使用者的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 404 (Not Found) 給客戶端。
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<MessageVO> handleUserNotFoundException(UserNotFoundException exception) {
        logger.error("找不到結果: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResult);
    }

    /**
     * 處理嘗試變更管理員帳號狀態的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 403 (Forbidden) 給客戶端。
     */
    @ExceptionHandler(AdminStatusModificationException.class)
    public ResponseEntity<MessageVO> handleAdminStatusModificationException(
            AdminStatusModificationException exception) {
        logger.error("嘗試變更管理員帳號狀態: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResult);
    }

    /**
     * 處理上傳頭像格式或大小不合法的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(InvalidAvatarException.class)
    public ResponseEntity<MessageVO> handleInvalidAvatarException(InvalidAvatarException exception) {
        logger.error("頭像不合法: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResult);
    }

    /**
     * 處理帳號或密碼驗證失敗的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 401 (Unauthorized) 給客戶端。
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<MessageVO> handleInvalidCredentialsException(InvalidCredentialsException exception) {
        logger.error("身分驗證失敗: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResult);
    }

    // ----- Metro -----

    /**
     * 處理找不到指定車站的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 404 (Not Found) 給客戶端。
     */
    @ExceptionHandler(StationNotFoundException.class)
    public ResponseEntity<MessageVO> handleStationNotFoundException(StationNotFoundException exception) {
        logger.error("找不到結果: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResult);
    }

    /**
     * 處理票價同步已在進行中、重複觸發的例外。
     * 註：同步進行中屬「資源狀態衝突」，固定回 409 Conflict。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 409 (Conflict) 給客戶端。
     */
    @ExceptionHandler(SyncInProgressException.class)
    public ResponseEntity<MessageVO> handleSyncInProgressException(SyncInProgressException exception) {
        logger.error("票價同步進行中，重複觸發: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResult);
    }

    // ----- Station Chat -----

    /**
     * 處理車站聊天留言夾帶非文字內容（圖片、base64 或 HTML 標籤）的例外。
     * 註：發送留言走 STOMP，此例外實際由 {@code StationChatStompController} 的 {@code @MessageExceptionHandler}
     * 送至 {@code /user/queue/errors}；此處 handler 供未來 REST 化與統一風格使用。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(InvalidChatContentException.class)
    public ResponseEntity<MessageVO> handleInvalidChatContentException(InvalidChatContentException exception) {
        logger.error("聊天留言內容不合法: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResult);
    }

    /**
     * 處理找不到指定車站聊天公告或留言的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 404 (Not Found) 給客戶端。
     */
    @ExceptionHandler(StationChatNotFoundException.class)
    public ResponseEntity<MessageVO> handleStationChatNotFoundException(StationChatNotFoundException exception) {
        logger.error("找不到結果: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResult);
    }

    /**
     * 處理匯出車站當日聊天紀錄 excel 檔失敗的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 500 (Internal Server Error) 給客戶端。
     */
    @ExceptionHandler(StationChatExportExcelFailedException.class)
    public ResponseEntity<MessageVO> handleStationChatExportExcelFailedException(
            StationChatExportExcelFailedException exception) {
        logger.error("匯出車站當日聊天紀錄 excel 檔失敗: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResult);
    }

    // ----- Station Bookmark -----

    /**
     * 處理找不到指定車站書籤的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 404 (Not Found) 給客戶端。
     */
    @ExceptionHandler(BookmarkNotFoundException.class)
    public ResponseEntity<MessageVO> handleBookmarkNotFoundException(BookmarkNotFoundException exception) {
        logger.error("找不到結果: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResult);
    }

    /**
     * 處理使用者對同一車站重複建立書籤的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(BookmarkDuplicateException.class)
    public ResponseEntity<MessageVO> handleBookmarkDuplicateException(BookmarkDuplicateException exception) {
        logger.error("不能重複建立: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResult);
    }

    /**
     * 處理車站書籤數量已達會員等級上限的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(BookmarkLimitExceededException.class)
    public ResponseEntity<MessageVO> handleBookmarkLimitExceededException(BookmarkLimitExceededException exception) {
        logger.error("已達車站書籤數量上限: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResult);
    }

    /**
     * 處理匯出車站書籤 excel 檔失敗的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 500 (Internal Server Error) 給客戶端。
     */
    @ExceptionHandler(StationBookmarkExportExcelFailedException.class)
    public ResponseEntity<MessageVO> handleStationBookmarkExportExcelFailedException(
            StationBookmarkExportExcelFailedException exception) {
        logger.error("匯出車站書籤 excel 檔失敗: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResult);
    }

    // ----- Trip Plan -----

    /**
     * 處理找不到指定旅程規劃的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 404 (Not Found) 給客戶端。
     */
    @ExceptionHandler(TripPlanNotFoundException.class)
    public ResponseEntity<MessageVO> handleTripPlanNotFoundException(TripPlanNotFoundException exception) {
        logger.error("找不到結果: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResult);
    }

    /**
     * 處理使用者操作非本人旅程規劃的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 403 (Forbidden) 給客戶端。
     */
    @ExceptionHandler(TripPlanAccessDeniedException.class)
    public ResponseEntity<MessageVO> handleTripPlanAccessDeniedException(TripPlanAccessDeniedException exception) {
        logger.error("存取被拒絕: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResult);
    }

    /**
     * 處理旅程規劃名稱重複的例外。
     * 註：重複名稱屬「資源狀態衝突」，本例外固定回 409 Conflict（不沿用重複建立類例外的 400）。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 409 (Conflict) 給客戶端。
     */
    @ExceptionHandler(TripPlanNameDuplicationException.class)
    public ResponseEntity<MessageVO> handleTripPlanNameDuplicationException(
            TripPlanNameDuplicationException exception) {
        logger.error("旅程規劃名稱重複: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResult);
    }

    /**
     * 處理資料庫完整性約束違反的例外。
     * 併發請求可能同時通過 service 的名稱重複預檢，最終由 filtered unique index
     * {@code UQ_user_trip_plans_user_id_name_active} 擋下，此處轉譯為與預檢一致的 409 訊息。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體；旅程規劃名稱唯一索引違反時回傳 HTTP status code 409 (Conflict)，
     *         其餘完整性違反回傳 500 (Internal Server Error) 給客戶端。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageVO> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception) {
        String rootCauseMessage = exception.getMostSpecificCause().getMessage();

        if (rootCauseMessage != null && rootCauseMessage.contains("UQ_user_trip_plans_user_id_name_active")) {
            logger.error("旅程規劃名稱重複（唯一索引擋下併發寫入）: {}", rootCauseMessage);

            MessageVO apiResult = new MessageVO("已有相同名稱的旅程規劃，請改用其他名稱!");

            return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResult);
        }

        logger.error("發生資料庫完整性約束違反的錯誤: {}", rootCauseMessage);

        MessageVO apiResult = new MessageVO("資料寫入失敗，請稍後再試!");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResult);
    }

    /**
     * 處理旅程規劃數量已達會員等級上限的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(TripPlanLimitExceededException.class)
    public ResponseEntity<MessageVO> handleTripPlanLimitExceededException(TripPlanLimitExceededException exception) {
        logger.error("已達旅程規劃數量上限: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResult);
    }

    /**
     * 處理匯出旅程規劃 excel 檔失敗的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 500 (Internal Server Error) 給客戶端。
     */
    @ExceptionHandler(TripPlanExportExcelFailedException.class)
    public ResponseEntity<MessageVO> handleTripPlanExportExcelFailedException(
            TripPlanExportExcelFailedException exception) {
        logger.error("匯出旅程規劃 excel 檔失敗: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResult);
    }
}
