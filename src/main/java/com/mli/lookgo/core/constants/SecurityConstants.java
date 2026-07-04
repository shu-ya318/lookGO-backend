package com.mli.lookgo.core.constants;

/**
 * 定義 Spring Security 相關的常數，包含公開 API 路徑清單。
 *
 * @author D5042101
 * @since 2026.06.06
 */
public class SecurityConstants {

    public static final String[] API_PUBLIC_ALL = {
            "/actuator/health",
            "/api/v1/auth/sign-up",
            "/api/v1/auth/log-in",
            "/api/v1/auth/forget-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh-tokens",
            "/api/v1/metro/get-metro-map",
            "/api/v1/metro/get-all-line",
            "/api/v1/metro/get-all-line-transfer",
            "/api/v1/metro/get-all-station",
            "/api/v1/metro/get-all-line-station",
            "/api/v1/metro/get-all-station-fare",
            "/api/v1/metro/get-station-by-code",
            "/api/v1/metro/get-origin-destination-detail",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/ws/**"
    };
}
