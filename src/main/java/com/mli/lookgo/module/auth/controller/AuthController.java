package com.mli.lookgo.module.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mli.lookgo.module.auth.model.dto.LoginDTO;
import com.mli.lookgo.module.auth.model.dto.SignupDTO;
import com.mli.lookgo.module.auth.model.vo.AuthVO;
import com.mli.lookgo.module.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * 處理使用者身分驗證相關的 HTTP 請求的介面層。負責驗證請求參數，把資料傳給業務層處理，最後封裝結果為 HTTP 回應回傳給客戶端。
 *
 * @author D5042101
 * @since 2026.5.30
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "處理身分驗證相關操作的 API")
public class AuthController {

    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入身分驗證相關的業務層 {@link AuthService}。
     *
     * @param authService
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 輸入使用者相關資訊來建立帳號，建立成功後自動登入並回傳存取憑證。
     *
     * @param signupDTO
     * @param response
     * @return ResponseEntity<AuthVO>
     */
    @Operation(summary = "使用者註冊", description = "依據輸入的使用者資訊建立帳號，成功後回傳存取憑證")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功建立帳號並取得存取憑證", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthVO.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "Email 已被使用，請換一個 email!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/signup")
    public ResponseEntity<AuthVO> signup(@Valid @RequestBody SignupDTO signupDTO, HttpServletResponse response) {
        logger.info("收到使用者註冊的請求，輸入內容: {}", signupDTO);
        AuthVO authVO = authService.signup(signupDTO, response);

        return ResponseEntity.ok(authVO);
    }

    /**
     * 輸入使用者帳號與密碼進行登入，驗證成功後回傳存取憑證。
     *
     * @param loginDTO
     * @param response
     * @return ResponseEntity<AuthVO>
     */
    @Operation(summary = "使用者登入", description = "依據輸入的帳號與密碼進行身分驗證，成功後回傳存取憑證")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功登入並取得存取憑證", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthVO.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入有效的帳號與密碼!"))),
            @ApiResponse(responseCode = "401", description = "帳號或密碼錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "帳號或密碼錯誤!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/login")
    public ResponseEntity<AuthVO> signin(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        logger.info("收到使用者登入的請求，輸入內容: {}", loginDTO);
        AuthVO authVO = authService.signin(loginDTO, response);

        return ResponseEntity.ok(authVO);
    }

    /**
     * 使用 Cookie 中的刷新令牌核發新的存取憑證與刷新憑證。
     *
     * @param request
     * @param response
     * @return ResponseEntity<AuthVO>
     */
    @Operation(summary = "刷新憑證", description = "驗證 HttpOnly Cookie 中的刷新令牌，成功後核發新的存取憑證與刷新憑證")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功刷新並取得新的憑證", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthVO.class))),
            @ApiResponse(responseCode = "401", description = "刷新令牌無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "刷新令牌無效或已過期!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/refresh")
    public ResponseEntity<AuthVO> refresh(HttpServletRequest request, HttpServletResponse response) {
        logger.info("收到刷新憑證的請求");
        AuthVO authVO = authService.refresh(request, response);

        return ResponseEntity.ok(authVO);
    }
}
