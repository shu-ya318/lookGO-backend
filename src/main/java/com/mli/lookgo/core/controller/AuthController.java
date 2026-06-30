package com.mli.lookgo.core.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mli.lookgo.core.exceptions.InvalidCredentialsException;
import com.mli.lookgo.core.model.dto.ForgetPasswordDTO;
import com.mli.lookgo.core.model.dto.LoginDTO;
import com.mli.lookgo.core.model.dto.ResetPasswordDTO;
import com.mli.lookgo.core.model.dto.SignupDTO;
import com.mli.lookgo.core.model.vo.AuthVO;
import com.mli.lookgo.core.model.vo.ForgetPasswordVO;
import com.mli.lookgo.core.result.MessageVO;
import com.mli.lookgo.core.security.CookieUtil;
import com.mli.lookgo.core.security.JwtUtil;
import com.mli.lookgo.core.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * @since 2026.06.06
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "處理身分驗證相關操作的 API")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入身分驗證相關的業務層 {@link AuthService}。
     *
     * @param authService
     * @param cookieUtil
     * @param jwtUtil
     */
    public AuthController(AuthService authService, CookieUtil cookieUtil, JwtUtil jwtUtil) {
        this.authService = authService;
        this.cookieUtil = cookieUtil;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 輸入使用者相關資訊來建立帳號，建立成功後自動登入並回傳存取token。
     *
     * @param signupDTO
     * @param httpServletResponse
     * @return ResponseEntity<AuthVO>
     */
    @Operation(summary = "使用者註冊", description = "依據輸入的使用者資訊建立帳號，成功後回傳存取token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功建立帳號並取得存取token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthVO.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "電子郵件已被使用，請換一個電子郵件!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/sign-up")
    public ResponseEntity<AuthVO> signup(@Valid @RequestBody SignupDTO signupDTO,
            HttpServletResponse httpServletResponse) {
        logger.debug("收到使用者註冊的請求，輸入內容: {}", signupDTO);
        AuthVO authVO = authService.signup(signupDTO, httpServletResponse);
        cookieUtil.addRefreshTokenCookie(httpServletResponse, authVO.getRefreshToken());

        return ResponseEntity.ok(authVO);
    }

    /**
     * 輸入使用者帳號與密碼進行登入，驗證成功後回傳存取token。
     *
     * @param loginDTO
     * @param httpServletResponse
     * @return ResponseEntity<AuthVO>
     */
    @Operation(summary = "使用者登入", description = "依據輸入的帳號與密碼進行身分驗證，成功後回傳存取token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功登入並取得存取token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthVO.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入有效的帳號與密碼!"))),
            @ApiResponse(responseCode = "401", description = "帳號或密碼錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "帳號或密碼錯誤!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/log-in")
    public ResponseEntity<AuthVO> login(@Valid @RequestBody LoginDTO loginDTO,
            HttpServletResponse httpServletResponse) {
        logger.debug("收到使用者登入的請求，輸入內容: {}", loginDTO);
        AuthVO authVO = authService.login(loginDTO, httpServletResponse);
        cookieUtil.addRefreshTokenCookie(httpServletResponse, authVO.getRefreshToken());

        return ResponseEntity.ok(authVO);
    }

    /**
     * 傳入 email 與 cellphone 進行驗證，通過後回傳重設密碼 token（有效期 15 分鐘）。
     *
     * @param forgetPasswordDTO
     * @return ResponseEntity<ForgetPasswordVO>
     */
    @Operation(summary = "請求重設密碼", description = "驗證 email 與 cellphone 是否與帳號資料一致，通過後回傳重設密碼 token（有效期 15 分鐘）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "驗證成功，回傳重設密碼 token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ForgetPasswordVO.class))),
            @ApiResponse(responseCode = "400", description = "請求參數格式錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入 Email!"))),
            @ApiResponse(responseCode = "401", description = "email 或 cellphone 驗證失敗", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "電子郵件或電話號碼驗證失敗!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/forget-password")
    public ResponseEntity<ForgetPasswordVO> forgetPassword(@Valid @RequestBody ForgetPasswordDTO forgetPasswordDTO) {
        logger.debug("收到忘記密碼的請求，email: {}", forgetPasswordDTO.getEmail());
        ForgetPasswordVO forgetPasswordVO = authService.forgetPassword(forgetPasswordDTO);

        return ResponseEntity.ok(forgetPasswordVO);
    }

    /**
     * 傳入重設密碼 token（query parameter）與新密碼，驗證通過後更新密碼。
     *
     * @param token            重設密碼 token（由 forget-password 取得）
     * @param resetPasswordDTO
     * @return ResponseEntity<MessageVO>
     */
    @Operation(summary = "重設密碼", description = "傳入有效的重設密碼 token（query parameter）與新密碼，驗證通過後更新使用者密碼")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密碼重設成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
            @ApiResponse(responseCode = "400", description = "請求參數格式錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "新密碼長度必須為 8-20 個字元!"))),
            @ApiResponse(responseCode = "401", description = "token 無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "重設密碼token無效或已過期!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/reset-password")
    public ResponseEntity<MessageVO> resetPassword(
            @Parameter(description = "重設密碼 token（由 forget-password 取得）", required = true)
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        logger.debug("收到重設密碼的請求");
        MessageVO apiResult = authService.resetPassword(token, resetPasswordDTO);

        return ResponseEntity.ok(apiResult);
    }

    /**
     * 使用 Cookie 中的刷新token核發新的存取token與刷新token。
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @return ResponseEntity<AuthVO>
     */
    @Operation(summary = "刷新token", description = "驗證 HttpOnly Cookie 中的刷新token，成功後核發新的存取token與刷新token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功刷新並取得新的token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthVO.class))),
            @ApiResponse(responseCode = "401", description = "刷新token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "刷新token無效或已過期!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/refresh-tokens")
    public ResponseEntity<AuthVO> refreshTokens(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        logger.debug("收到刷新token的請求");
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(httpServletRequest);
        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("刷新token無效或已過期!");
        }

        AuthVO authVO = authService.refreshTokens(refreshToken);
        cookieUtil.addRefreshTokenCookie(httpServletResponse, authVO.getRefreshToken());

        return ResponseEntity.ok(authVO);
    }

    /**
     * 把使用者登出並清除token。
     * 為了讓 Swagger UI 能正常調用這支 API 而定義。
     * 實際登出功能由 JwtLogoutSuccessHandler 處理，在前端以呼叫 /log-out API 作為登出觸發。
     */
    @Operation(summary = "使用者登出", description = "把使用者登出並清除token")
    @PostMapping("/log-out")
    public void logout() {
    }
}
