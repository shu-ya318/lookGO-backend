package com.mli.lookgo.module.auth.security;

import org.springframework.stereotype.Component;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 處理 Cookie 操作的工具類別，提供刷新令牌 Cookie 的新增與清除功能。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Component
public class CookieUtil {

    private final JwtUtil jwtUtil;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入 JWT 工具 {@link JwtUtil}。
     *
     * @param jwtUtil
     */
    public CookieUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 在 HTTP 回應中新增刷新令牌的 Cookie。
     *
     * @param response     HTTP 回應
     * @param refreshToken 刷新令牌字串
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken).httpOnly(true).secure(true).path("/")
                .maxAge(jwtUtil.getRefreshTokenExpiration() / 1000).sameSite("None").build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 從 HTTP 請求的 Cookie 中取得刷新令牌。
     *
     * @param request HTTP 請求
     * @return 刷新令牌字串，若不存在則回傳 null
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 清除 HTTP 回應中的刷新令牌 Cookie。
     *
     * @param response HTTP 回應
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "").httpOnly(true).secure(true).path("/").maxAge(0)
                .sameSite("None").build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
