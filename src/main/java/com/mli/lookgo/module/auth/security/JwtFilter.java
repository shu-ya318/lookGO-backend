package com.mli.lookgo.module.auth.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mli.lookgo.common.constants.SecurityConstants;
import com.mli.lookgo.module.auth.service.RedisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 攔截每次 HTTP 請求，驗證請求標頭中的 JWT 存取憑證，並在通過驗證後設定 Spring Security 的身分驗證資訊。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final RedisService redisService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入 JWT 工具 {@link JwtUtil} 與使用者詳細資料服務
     * {@link UserDetailsService}。
     *
     * @param jwtUtil
     * @param userDetailsService
     */
    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, RedisService redisService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.redisService = redisService;
    }

    /**
     * 判斷是否跳過此 Filter，符合公開 API 路徑的請求不進行 JWT 驗證。
     *
     * @param httpServletRequest
     * @return 是否跳過 Filter
     * @throws ServletException
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest httpServletRequest) throws ServletException {
        String path = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());

        for (String pattern : SecurityConstants.API_PUBLIC_ALL) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 從請求標頭中取得 JWT 存取憑證，驗證有效後將身分驗證資訊寫入 Spring Security Context。
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest httpServletRequest,
            @NonNull HttpServletResponse httpServletResponse,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestUri = httpServletRequest.getRequestURI();
        String method = httpServletRequest.getMethod();
        logger.debug("[JwtFilter] 收到請求: {} {}", method, requestUri);

        String bearerToken = httpServletRequest.getHeader("Authorization");

        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            logger.debug("[JwtFilter] 未帶有效 Authorization header，跳過 JWT 驗證 (uri={})", requestUri);
            filterChain.doFilter(httpServletRequest, httpServletResponse);

            return;
        }

        logger.debug("[JwtFilter] 偵測到 Bearer token，開始驗證 (uri={})", requestUri);

        try {
            String authToken = bearerToken.substring(7);

            boolean isValid = jwtUtil.validateAccessToken(authToken);
            logger.debug("[JwtFilter] JWT 驗證結果: {} (uri={})", isValid, requestUri);

            if (isValid) {
                String jti = jwtUtil.getJtiFromToken(authToken);
                logger.debug("[JwtFilter] JWT jti={}", jti);

                boolean isBlacklisted = redisService.isAccessTokenJtiInBlacklist(jti);
                logger.debug("[JwtFilter] JWT 是否在黑名單: {} (jti={})", isBlacklisted, jti);

                if (isBlacklisted) {
                    logger.warn("[JwtFilter] 憑證已被列入黑名單，拒絕存取! (jti={})", jti);
                    filterChain.doFilter(httpServletRequest, httpServletResponse);

                    return;
                }

                String username = jwtUtil.getEmailFromAccessToken(authToken);
                logger.debug("[JwtFilter] 從 JWT 解析出 email(username)={}", username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                logger.debug("[JwtFilter] 載入 UserDetails 成功，username={}, authorities={}",
                        userDetails.getUsername(), userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                logger.debug("[JwtFilter] SecurityContext 已設定，principal={}, authorities={}",
                        authenticationToken.getName(), authenticationToken.getAuthorities());
            } else {
                logger.debug("[JwtFilter] JWT 驗證失敗，SecurityContext 不會被設定 (uri={})", requestUri);
            }
        } catch (Exception error) {
            logger.error("[JwtFilter] 無法設定使用者身分驗證: {} (uri={})", error.getMessage(), requestUri, error);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
