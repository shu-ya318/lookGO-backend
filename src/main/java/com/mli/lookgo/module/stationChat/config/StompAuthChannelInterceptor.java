package com.mli.lookgo.module.stationChat.config;

import com.mli.lookgo.core.security.JwtUtil;
import com.mli.lookgo.core.security.UserDetailsServiceImpl;
import com.mli.lookgo.core.security.JwtFilter;
import com.mli.lookgo.core.service.RedisService;
import com.mli.lookgo.module.stationChat.exceptions.StompAuthException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 處理 STOMP CONNECT 階段的 JWT 驗證攔截器。
 * 比照 {@link JwtFilter} 的驗證流程，但只需 CONNECT 階段執行一次，驗證通過後綁定 Principal 供整個連線生命週期使用。
 * 
 * @author D5042101
 * @since 2026.07.03
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final RedisService redisService;

    public StompAuthChannelInterceptor(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService,
            RedisService redisService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.redisService = redisService;
    }

    /**
     * 於訊息送入 Channel 前執行；僅針對 CONNECT 指令做驗證，其餘指令直接放行
     * （SEND／SUBSCRIBE 階段的授權交由 Spring Security 既有機制或 Service 層業務邏輯處理）。
     *
     * @param message
     * @param channel
     * @return 原始或處理過的訊息
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(stompHeaderAccessor.getCommand())) {
            logger.debug("[StompAuthChannelInterceptor] 收到 CONNECT 請求，開始驗證");

            String authToken = resolveToken(stompHeaderAccessor);

            if (authToken == null) {
                logger.debug("[StompAuthChannelInterceptor] 缺少 Authorization header，拒絕連線");
                throw new StompAuthException("缺少認證 Token，拒絕建立 WebSocket 連線!");
            }

            try {
                // 檢查有效性
                boolean isValid = jwtUtil.validateAccessToken(authToken);
                logger.debug("[StompAuthChannelInterceptor] token驗證結果: {}", isValid);

                if (!isValid) {
                    throw new StompAuthException("token驗證失敗，拒絕建立 WebSocket 連線");
                }

                // 檢查是否在黑名單
                String jti = jwtUtil.getJtiFromToken(authToken);
                boolean isBlacklisted = redisService.isAccessTokenJtiInBlacklist(jti);
                logger.debug("[StompAuthChannelInterceptor] token是否在黑名單: {} (jti={})", isBlacklisted, jti);

                if (isBlacklisted) {
                    logger.warn("[StompAuthChannelInterceptor] token已列入黑名單，拒絕連線 (jti={})", jti);
                    throw new StompAuthException("token已失效，拒絕建立 WebSocket 連線");
                }

                // 實際使用者資訊
                String email = jwtUtil.getEmailFromAccessToken(authToken);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                stompHeaderAccessor.setUser(authentication);

                logger.debug("STOMP CONNECT 驗證成功，使用者: {}", email);
            } catch (StompAuthException error) {
                throw error;
            } catch (Exception error) {
                logger.error("STOMP CONNECT Token 驗證失敗: {}", error.getMessage());
                throw new StompAuthException("Token 驗證失敗，拒絕建立 WebSocket 連線", error);
            }
        }

        return message;
    }

    /**
     * 從 STOMP CONNECT frame 的自訂 header 取出 Bearer token。
     *
     * @param stompHeaderAccessor
     * @return 純 accessToken 字串，取不到則回傳 null
     */
    private String resolveToken(StompHeaderAccessor stompHeaderAccessor) {
        String header = stompHeaderAccessor.getFirstNativeHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
