package com.mli.lookgo.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.module.metro.exceptions.StationNotFoundException;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkDuplicateException;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkLimitExceededException;
import com.mli.lookgo.module.stationBookmark.exceptions.BookmarkNotFoundException;
import com.mli.lookgo.module.stationBookmark.exceptions.StationBookmarkExportExcelFailedException;
import com.mli.lookgo.module.stationChat.exceptions.StationChatExportExcelFailedException;
import com.mli.lookgo.module.stationChat.exceptions.StationChatNotFoundException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanAccessDeniedException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanExportExcelFailedException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanLimitExceededException;
import com.mli.lookgo.module.tripPlan.exceptions.TripPlanNotFoundException;
import com.mli.lookgo.module.user.exceptions.AdminStatusModificationException;
import com.mli.lookgo.module.user.exceptions.MembershipUpgradeException;
import com.mli.lookgo.module.user.exceptions.UserNotFoundException;

import java.util.HashMap;
import java.util.Map;

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
     * 處理不符合會員等級升級條件的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 409 (Conflict) 給客戶端。
     */
    @ExceptionHandler(MembershipUpgradeException.class)
    public ResponseEntity<MessageVO> handleMembershipUpgradeException(MembershipUpgradeException exception) {
        logger.error("會員等級升級失敗: {}", exception.getMessage());

        MessageVO apiResult = new MessageVO(exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResult);
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

    // ----- Station Chat -----

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
