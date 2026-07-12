package com.mli.lookgo.core.config;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mli.lookgo.core.constants.SecurityConstants;
import com.mli.lookgo.core.security.JwtFilter;
import com.mli.lookgo.core.security.LogoutResultHandler;

/**
 * 處理 Spring Security 的客製化配置。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final LogoutResultHandler logoutResultHandler;
    private final List<String> allowedOrigins;

    private static final ObjectMapper OBJECTMAPPER = new ObjectMapper();

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param jwtFilter
     * @param logoutResultHandler
     * @param allowedOrigins
     */
    public SecurityConfig(JwtFilter jwtFilter, LogoutResultHandler logoutResultHandler,
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.logoutResultHandler = logoutResultHandler;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * 設定 HTTP 安全過濾鏈，執行時期請求依序經過：
     * 1. CORS 跨域驗證（同時關閉 CSRF）
     * 2. 攔截登出 URL，符合條件則執行登出
     * 3. 設定為 無狀態 Session，透過 JWT Token 解析與身分驗證
     * 4. 依照規則判斷是否需要授權（公開路由直接放行，其餘需驗證）
     * 若驗證或授權失敗，統一回傳 401 錯誤。
     *
     * @param httpSecurity
     * @return SecurityFilterChain
     * @throws Exception
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                            String json = OBJECTMAPPER
                                    .writeValueAsString(Map.of("message", "未授權錯誤，token無效或已過期!"));
                            response.getWriter().write(json);
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize.requestMatchers(SecurityConstants.API_PUBLIC_ALL)
                        .permitAll().anyRequest().authenticated())
                .logout(logout -> logout.logoutUrl("/api/v1/auth/log-out").logoutSuccessHandler(logoutResultHandler));

        return httpSecurity.build();
    }

    /**
     * 設定 CORS 跨來源資源共享的規則。
     * 當 securityFilterChain 中呼叫 .cors(Customizer.withDefaults()) 時被注入使用。
     *
     * @return CorsConfigurationSource
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.setAllowedOriginPatterns(allowedOrigins); // patterns，支援萬用字元比對
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setAllowCredentials(true); // 讓跨域請求可附加 Cookie / Authorization Header
        corsConfiguration.setMaxAge(3600L); // OPTIONS 預檢請求的結果快取 3600 秒（1小時）

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration); // 讓所有路徑套用統一的 CORS 設定

        return urlBasedCorsConfigurationSource;
    }

    /**
     * 防止 客製化配置的 JwtFilter 被自動註冊到 Servlet 容器（Tomcat）的原生 Filter Chain。
     * 避免每個請求執行兩次 JwtFilter。
     *
     * @param jwtFilter
     * @return FilterRegistrationBean
     */
    @Bean
    FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter jwtFilter) {
        FilterRegistrationBean<JwtFilter> filterRegistrationBean = new FilterRegistrationBean<>(jwtFilter);

        filterRegistrationBean.setEnabled(false);

        return filterRegistrationBean;
    }

    /**
     * 建立 BCrypt 密碼加密器的 Bean，供密碼加密與驗證使用。
     *
     * @return PasswordEncoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
