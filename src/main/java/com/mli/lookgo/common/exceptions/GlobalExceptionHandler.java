package com.mli.lookgo.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mli.lookgo.common.result.ApiResult;
import com.mli.lookgo.module.auth.exceptions.InvalidCredentialsException;
import com.mli.lookgo.module.auth.exceptions.UserDuplicateException;
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
        logger.error("錯誤細節: ", exception);

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
    public ResponseEntity<ApiResult> handleIllegalArgumentException(IllegalArgumentException exception) {
        logger.error("發生非法請求參數例外的錯誤: {}", exception.getMessage());
        logger.error("錯誤細節: ", exception);

        ApiResult apiErrorResponse = new ApiResult(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiErrorResponse);
    }

    /**
     * 處理執行時期相關的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 500 (Internal Server Error) 給客戶端。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResult> handleRuntimeException(RuntimeException exception) {
        logger.error("發生執行時期例外的錯誤: {}", exception.getMessage());
        logger.error("錯誤細節: ", exception);

        ApiResult apiErrorResponse = new ApiResult(exception.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiErrorResponse);
    }

    // ----- 權限相關 -----

    /**
     * 處理存取被拒絕的例外（權限不足）。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 403 (Forbidden) 給客戶端。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult> handleAccessDeniedException(AccessDeniedException exception) {
        logger.error("存取被拒絕: {}", exception.getMessage());
        logger.error("錯誤細節: ", exception);

        ApiResult apiResult = new ApiResult("權限不足，無法操作!");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResult);
    }

    // ----- 使用者相關 -----

    /**
     * 處理使用者重複建立的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 400 (Bad Request) 給客戶端。
     */
    @ExceptionHandler(UserDuplicateException.class)
    public ResponseEntity<ApiResult> handleUserDuplicateException(UserDuplicateException exception) {
        logger.error("不能重複建立: {}", exception.getMessage());
        logger.error("錯誤細節: ", exception);

        ApiResult apiResult = new ApiResult(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResult);
    }

    /**
     * 處理找不到使用者的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 404 (Not Found) 給客戶端。
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResult> handleUserNotFoundException(UserNotFoundException exception) {
        logger.error("找不到結果: {}", exception.getMessage());
        logger.error("錯誤細節: ", exception);

        ApiResult apiResult = new ApiResult(exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResult);
    }

    /**
     * 處理帳號或密碼驗證失敗的例外。
     *
     * @param exception
     * @return 包含具體錯誤訊息的回應實體，並回傳 HTTP status code 401 (Unauthorized) 給客戶端。
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResult> handleInvalidCredentialsException(InvalidCredentialsException exception) {
        logger.error("身分驗證失敗: {}", exception.getMessage());
        logger.error("錯誤細節: ", exception);

        ApiResult apiResult = new ApiResult(exception.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResult);
    }
}
