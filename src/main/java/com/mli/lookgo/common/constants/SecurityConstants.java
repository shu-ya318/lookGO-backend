package com.mli.lookgo.common.constants;

/**
 * 定義 Spring Security 相關的常數，包含公開 API 路徑清單。
 *
 * @author D5042101
 * @since 2026.06.06
 */
public class SecurityConstants {

    public static final String[] API_PUBLIC_ALL = {
            "/actuator/health",
            "/auth/signup",
            "/auth/login",
            "/auth/refresh",
            "/user/get-current-user",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };
}
