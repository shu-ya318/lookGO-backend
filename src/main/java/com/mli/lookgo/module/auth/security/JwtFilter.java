package com.mli.lookgo.module.auth.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mli.lookgo.common.constants.SecurityConstants;

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
 * 攔截每次 HTTP 請求，驗證請求標頭中的 JWT 存取令牌，並在通過驗證後設定 Spring Security 的身分驗證資訊。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入 JWT 工具 {@link JwtUtil} 與使用者詳細資料服務
     * {@link UserDetailsService}。
     *
     * @param jwtUtil            JWT 工具
     * @param userDetailsService 使用者詳細資料服務
     */
    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 判斷是否跳過此 Filter，符合公開 API 路徑的請求不進行 JWT 驗證。
     *
     * @param request HTTP 請求
     * @return 是否跳過此 Filter
     * @throws ServletException
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        for (String pattern : SecurityConstants.API_PUBLIC_ALL) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 從請求標頭中取得 JWT 存取令牌，驗證有效後將身分驗證資訊寫入 Spring Security Context。
     *
     * @param request     HTTP 請求
     * @param response    HTTP 回應
     * @param filterChain 過濾器鏈
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String bearerToken = request.getHeader("Authorization");

        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String authToken = bearerToken.substring(7);

            if (jwtUtil.validateAccessToken(authToken)) {
                String username = jwtUtil.getEmailFromToken(authToken);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception error) {
            logger.error("無法設定使用者身分驗證", error);

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
