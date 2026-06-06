package com.mli.lookgo.module.auth.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 處理使用者登出成功後的回應，清除刷新令牌 Cookie 並回傳結果訊息。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Component
public class LogoutResultHandler implements LogoutSuccessHandler {

    private final CookieUtil cookieUtil;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入 Cookie 工具 {@link CookieUtil}。
     *
     * @param cookieUtil
     */
    public LogoutResultHandler(CookieUtil cookieUtil) {
        this.cookieUtil = cookieUtil;
    }

    /**
     * 處理登出成功的事件，清除刷新令牌 Cookie 並回傳對應的訊息。
     *
     * @param request        HTTP 請求
     * @param response       HTTP 回應
     * @param authentication 身分驗證資訊
     * @throws IOException
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        try {
            cookieUtil.clearRefreshTokenCookie(response);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            MAPPER.writeValue(response.getWriter(), Map.of("message", "登出成功!"));

        } catch (Exception error) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            MAPPER.writeValue(response.getWriter(), Map.of("message", "登出失敗!"));
        }
    }
}
