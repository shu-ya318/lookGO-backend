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
 * 處理 Spring Security 的客製化配置，包含 CORS、JWT Filter、授權規則與登出機制。
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
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入所需的依賴。
     *
     * @param jwtFilter
     * @param logoutResultHandler
     * @param allowedOrigins CORS 允許的來源清單
     */
    public SecurityConfig(JwtFilter jwtFilter, LogoutResultHandler logoutResultHandler,
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtFilter = jwtFilter;
        this.logoutResultHandler = logoutResultHandler;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * 設定 HTTP 安全性過濾鏈，定義授權規則、JWT 過濾器與登出行為。
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

                            String json = MAPPER
                                    .writeValueAsString(Map.of("message", "未授權錯誤，token無效或已過期"));
                            response.getWriter().write(json);
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize.requestMatchers(SecurityConstants.API_PUBLIC_ALL)
                        .permitAll().anyRequest().authenticated())
                .logout(logout -> logout.logoutUrl("/api/v1/auth/log-out").logoutSuccessHandler(logoutResultHandler));

        return httpSecurity.build();
    }

    /**
     * 設定 CORS 跨來源資源共享的規則，允許前端指定來源存取 API。
     *
     * @return CorsConfigurationSource
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", config);

        return urlBasedCorsConfigurationSource;
    }

    /**
     * 防止 JWT 過濾器被 Spring Boot 自動注冊為 Servlet Filter，避免重複執行。
     *
     * @param jwtFilter
     * @return FilterRegistrationBean
     */
    @Bean
    FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter jwtFilter) {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>(jwtFilter);
        registration.setEnabled(false);

        return registration;
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
