package com.mli.lookgo.module.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mli.lookgo.common.result.ApiResult;
import com.mli.lookgo.module.auth.exceptions.InvalidCredentialsException;
import com.mli.lookgo.module.auth.model.dto.ForgetPasswordDTO;
// import com.mli.lookgo.module.auth.model.dto.ForgetPasswordDTO;
import com.mli.lookgo.module.auth.model.dto.LoginDTO;
import com.mli.lookgo.module.auth.model.dto.ResetPasswordDTO;
import com.mli.lookgo.module.auth.model.dto.SignupDTO;
import com.mli.lookgo.module.auth.model.vo.AuthVO;
import com.mli.lookgo.module.auth.security.CookieUtil;
import com.mli.lookgo.module.auth.security.JwtUtil;
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
     * 輸入電子郵件進行請求重設密碼，驗證成功後回傳請求成功訊息。
     *
     * @param forgetPasswordDTO
     * @return ResponseEntity<ApiResult>
     */
    @Operation(summary = "請求重設密碼", description = "發送密碼重設連結到電子郵件。(注意:不返回電子郵件有效性的驗證結果)(目前只提供寄送測試信件)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "請求成功(電子郵件有效才會收到信件)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthVO.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入有效的電子郵件!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/forget-password")
    public ResponseEntity<ApiResult> forgetPassword(@RequestBody ForgetPasswordDTO forgetPasswordDTO) {
        logger.debug("收到忘記密碼的請求");
        ApiResult apiresult = authService.forgetPassword(forgetPasswordDTO);

        return ResponseEntity.ok(apiresult);
    }

    /**
     * 輸入重設密碼token與新密碼，驗證通過後更新密碼並回傳成功訊息。
     *
     * @param resetPasswordDTO
     * @return ResponseEntity<ApiResult>
     */
    @Operation(summary = "重設密碼", description = "驗證重設密碼token，成功後更新使用者密碼")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密碼重設成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "請輸入合法的token與密碼!"))),
            @ApiResponse(responseCode = "401", description = "token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "重設密碼token無效或已過期!"))),
            @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResult> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        logger.debug("收到重設密碼的請求");
        ApiResult apiResult = authService.resetPassword(resetPasswordDTO);

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
