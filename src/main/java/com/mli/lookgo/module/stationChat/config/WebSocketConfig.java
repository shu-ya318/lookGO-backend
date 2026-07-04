package com.mli.lookgo.module.stationChat.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 處理 WebSocket 的客製化配置。
 *
 * @author D5042101
 * @since 2026.07.03
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * 註冊 STOMP handshake 端點。
     *
     * @param registry STOMP 端點註冊器
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }

    /**
     * 設定訊息代理路由。
     * /topic：Server 廣播給訂閱者（如 /topic/station-chat/{stationId}）
     * /app：Client 送出訊息前綴（如 /app/station-chat/{stationId}/send）
     * /user：@MessageExceptionHandler 透過 @SendToUser 回傳個人錯誤訊息所需
     *
     * @param registry 訊息代理註冊器
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 掛載 CONNECT 階段的 JWT 驗證攔截器。
     *
     * @param registration 入站 Channel 註冊器
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
