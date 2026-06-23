package com.mli.lookgo.module.auth.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mli.lookgo.module.auth.service.RedisService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 處理使用者登出成功後的回應，清除刷新token Cookie 並回傳結果訊息。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Component
public class LogoutResultHandler implements LogoutSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final CookieUtil cookieUtil;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入 Cookie 工具 {@link CookieUtil}。
     *
     * @param cookieUtil
     */
    public LogoutResultHandler(JwtUtil jwtUtil, RedisService redisService, CookieUtil cookieUtil) {
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
        this.cookieUtil = cookieUtil;
    }

    /**
     * 處理登出成功的事件，清除刷新token Cookie 並回傳對應的訊息。
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @param authentication
     * @throws IOException
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            Authentication authentication) throws IOException {
        try {
            String bearerToken = httpServletRequest.getHeader("Authorization");
            addValidTokenToBlacklist(bearerToken);

            String refreshToken = cookieUtil.getRefreshTokenFromCookie(httpServletRequest);
            if (refreshToken != null && jwtUtil.validateRefreshToken(refreshToken)) {
                String email = jwtUtil.getEmailFromRefreshToken(refreshToken);
                redisService.deleteRefreshTokenJti(email);
            }

            cookieUtil.clearRefreshTokenCookie(httpServletResponse);

            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            MAPPER.writeValue(httpServletResponse.getWriter(), Map.of("message", "登出成功!"));

        } catch (Exception error) {
            httpServletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            MAPPER.writeValue(httpServletResponse.getWriter(), Map.of("message", "登出失敗!"));
        }
    }

    private void addValidTokenToBlacklist(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String accessToken = bearerToken.substring(7);

            if (jwtUtil.validateAccessToken(accessToken)) {
                String jti = jwtUtil.getJtiFromToken(accessToken);
                long ttl = jwtUtil.getRemainingTtlFromAccessToken(accessToken);

                if (ttl > 0) {
                    redisService.saveAccessTokenJtiToBlacklist(jti, ttl, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}
