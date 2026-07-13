package com.mli.lookgo.module.stationChat.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins, ObjectMapper objectMapper) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.allowedOrigins = allowedOrigins;
        this.objectMapper = objectMapper;
    }

    /**
     * 1. HTTP Handshake 階段: 註冊 WebSocket 連線的進入點 /ws 並設定 CORS。
     * 讓瀏覽器發起 HTTP 請求，基於 TCP 建立連線後，改用 WebSocket 協定，以 STOMP 進行雙向長連線通訊。
     *
     * @param registry STOMP 端點註冊器
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }

    /**
     * 2. STOMP CONNECT (安全認證)階段: 掛載 CONNECT 階段的 JWT 驗證攔截器。
     *
     * @param registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    /**
     * 3. STOMP SUBSCRIBE (訂閱主題) 階段 + 4. STOMP SEND (訊息傳送) 階段: 設定訊息代理路由。
     * 當 Client 端訂閱特定前綴的訊息主題，訊息代理會將訊息廣播給訂閱者。
     * (省去 Websocket 自行定義訊息路由的步驟)
     * 
     * @param registry 訊息代理註冊器
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 設定 Client 端可訂閱的廣播路徑
        registry.enableSimpleBroker("/topic", "/queue");
        // 設定 Client 端傳送訊息的前綴路徑
        registry.setApplicationDestinationPrefixes("/app");
        // 設定目標使用者訊息的前綴路徑
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 4. STOMP SEND (訊息傳送) 階段: 設定 STOMP payload 的 JSON 轉換器。
     * (省去 Websocket 在每次接收訊息時自行解析 JSON 的步驟)
     *
     * @param messageConverters 訊息轉換器清單
     * @return false，表示不再疊加框架預設的轉換器
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);

        MappingJackson2MessageConverter jacksonConverter = new MappingJackson2MessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);
        jacksonConverter.setContentTypeResolver(resolver);

        messageConverters.add(new StringMessageConverter());
        messageConverters.add(new ByteArrayMessageConverter());
        messageConverters.add(jacksonConverter);

        return false;
    }
}
