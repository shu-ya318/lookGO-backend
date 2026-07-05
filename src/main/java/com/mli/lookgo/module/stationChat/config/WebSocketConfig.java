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

    /**
     * 讓 STOMP payload 的 JSON 轉換共用 Spring 管理的 {@link ObjectMapper}（{@code JacksonConfig} 客製化過），
     * 否則框架預設會另外建立一個全新、未套用任何客製化設定的 ObjectMapper，導致 HTTP 與 STOMP 的 JSON
     * 行為不一致（例如列舉數字誤判成 ordinal 的防呆設定不會套用到 STOMP）。
     *
     * @param messageConverters 訊息轉換器清單（初始為空）
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
