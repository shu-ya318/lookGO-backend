package com.mli.lookgo.module.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mli.lookgo.module.auth.model.vo.UserVO;
import com.mli.lookgo.module.auth.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 處理使用者相關的 HTTP 請求的介面層。負責驗證請求參數，把資料傳給業務層處理，最後封裝結果為 HTTP 回應回傳給客戶端。
 *
 * @author D5042101
 * @since 2026.5.30
 */
@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "處理使用者資料相關操作的 API")
public class UserController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入使用者相關的業務層 {@link UserService}。
     *
     * @param userService
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 解析 HttpOnly Cookie 中的刷新令牌，取得當前登入使用者的資訊。
     *
     * @param request
     * @return ResponseEntity<UserVO>
     */
    @Operation(summary = "取得當前使用者資訊", description = "解析 HttpOnly Cookie 中的刷新令牌，取得當前登入使用者的資訊")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功取得當前使用者的資訊", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserVO.class))),
            @ApiResponse(responseCode = "401", description = "刷新令牌無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "刷新令牌無效或已過期!"))),
            @ApiResponse(responseCode = "404", description = "找不到當前使用者", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "找不到當前使用者!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/get-current-user")
    public ResponseEntity<UserVO> getCurrentUser(HttpServletRequest request) {
        logger.info("收到查詢當前使用者資訊的請求");
        UserVO userVO = userService.getCurrentUser(request);

        return ResponseEntity.ok(userVO);
    }
}
