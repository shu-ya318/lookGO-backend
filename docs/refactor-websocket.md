# WebSocket 重構規劃：STOMP 改為原生 WebSocket

> 範圍：`module/stationChat`（`WebSocketConfig`、`StompAuthChannelInterceptor`、`StationChatStompController`、`StompAuthException`）＋ 少量 core 註解修正
>
> 撰寫日期：2026.07.19
>
> 目標：移除 STOMP 子協定與 Simple Broker，改為**原生 WebSocket 連線 + 自訂 JSON 訊息協定**，維持現有車站聊天室的所有功能執行效果。`StationChatService` 業務層**完全不動**。

---

## 一、背景與動機

現行車站聊天室使用 Spring 的 `@EnableWebSocketMessageBroker`（STOMP over WebSocket）：由 Simple Broker 管理訂閱與廣播、`@MessageMapping` 路由訊息、`MappingJackson2MessageConverter` 轉換 payload。

改為原生 WebSocket 後，這些框架職責改由專案自行實作。對企業級小型專案而言，主流做法是：

- `@EnableWebSocket` + `WebSocketConfigurer` 註冊單一 `TextWebSocketHandler`
- 以**自訂 JSON 訊息協定**（帶 `action` / `type` 欄位的信封格式）取代 STOMP frame
- 以 `ConcurrentHashMap` 維護「車站 → 訂閱 session」的登記表取代 Simple Broker
- 以**連線後首則 AUTH 訊息**完成 JWT 認證，取代 STOMP CONNECT header 認證
- 廣播時以 `ConcurrentWebSocketSessionDecorator` 包裝 session，確保並發寫入安全

功能總量不變（發送留言、刪除留言、按車站廣播、錯誤回發本人），僅通訊協定層改變。

---

## 二、現況盤點與功能對照

現行 STOMP 架構的每一項職責，都必須在原生 WebSocket 中有明確的對等實作：

| #   | 現行 STOMP 職責                                                     | 現行實作位置                               | 原生 WebSocket 對等實作                                       |
| --- | ------------------------------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------- |
| 1   | HTTP Handshake 進入點 `/ws` + CORS                                  | `WebSocketConfig.registerStompEndpoints()` | `WebSocketConfigurer.registerWebSocketHandlers()`（路徑不變） |
| 2   | CONNECT 階段 JWT 驗證（有效性、Redis 黑名單、載入使用者）           | `StompAuthChannelInterceptor`              | 首則 `AUTH` 訊息驗證（驗證邏輯**原封不動**搬移）              |
| 3   | 訂閱 `/topic/station-chat/{stationId}`                              | Simple Broker（`enableSimpleBroker`）      | `SUBSCRIBE` / `UNSUBSCRIBE` 訊息 + 自建訂閱登記表             |
| 4   | 發送留言 `/app/station-chat/{stationId}/send-message`               | `@MessageMapping` + `@Valid @Payload`      | `SEND_MESSAGE` 訊息 + 手動呼叫 `Validator`                    |
| 5   | 刪除留言 `/app/station-chat/{stationId}/delete-message/{messageId}` | `@MessageMapping` + `@DestinationVariable` | `DELETE_MESSAGE` 訊息（id 改放 JSON 欄位）                    |
| 6   | 廣播 `StationChatEventVO`（NEW / DELETE）給訂閱者                   | `SimpMessagingTemplate.convertAndSend()`   | 登記表遍歷 + 逐一 `sendMessage()`                             |
| 7   | 表單驗證錯誤回發本人（欄位→訊息 Map）                               | `@MessageExceptionHandler` + `@SendToUser` | `ERROR` 訊息直接回寫觸發的 session                            |
| 8   | JSON 轉換錯誤回發本人（`MessageVO`，不暴露 Jackson 細節）           | 同上                                       | 同上（catch `JsonProcessingException`）                       |
| 9   | 其餘業務例外回發本人（`MessageVO(exception.getMessage())`）         | 同上                                       | 同上（catch `RuntimeException`）                              |
| 10  | JSON 序列化使用專案共用 `ObjectMapper`                              | `configureMessageConverters()`             | Handler 注入同一顆 `ObjectMapper`                             |

> 註 1：現行 Simple Broker **未設定 heartbeat**（未提供 TaskScheduler，預設 `0,0`），因此無心跳功能需要對等維持；連線存活交由容器的 idle timeout 管理即可。
>
> 註 2：`@SendToUser` 預設會發給該使用者的所有 session；改為原生後錯誤只回寫**觸發指令的那條連線**。對「發送者本人看到錯誤」的實際效果而言相同，且語意更精準。

不受影響的部分：

- `StationChatService`、`StationChatDAO`、`StationChatMapper.xml` —— 完全不動
- `StationChatController`（REST：歷史留言、公告、匯出 excel）—— 完全不動
- `StationChatEventVO`、`SendMessageDTO`、`ChatEventTypeEnum`、`ChatTypeEnum` —— 結構不動（僅改 Javadoc 中的 STOMP 字樣）
- `SecurityConstants` 的 `/ws/**` 公開規則 —— 不動（handshake 仍為匿名，認證發生在連線後首則訊息）

---

## 三、自訂訊息協定設計

STOMP 移除後，frame 格式由專案自訂。所有訊息皆為 JSON 文字訊息（TextMessage）。

### 3.1 Client → Server（以 `action` 區分）

```json
{ "action": "AUTH", "token": "eyJhbGciOi..." }

{ "action": "SUBSCRIBE", "stationId": 5 }

{ "action": "UNSUBSCRIBE", "stationId": 5 }

{ "action": "SEND_MESSAGE", "stationId": 5,
  "payload": { "chatType": "TEXT", "content": "這裡的電梯正在維修" } }

{ "action": "DELETE_MESSAGE", "stationId": 5, "messageId": 12 }
```

- `AUTH.token` 為純 accessToken 字串（不含 `Bearer ` 前綴，簡化前端組裝）
- `SEND_MESSAGE.payload` 即現行 `SendMessageDTO`，欄位與驗證規則完全沿用
- 一條連線可同時訂閱多個車站（與 STOMP 多 SUBSCRIBE 行為一致）

### 3.2 Server → Client（以 `type` 區分）

```json
{ "type": "AUTH_SUCCESS" }

{ "type": "SUBSCRIBED", "stationId": 5 }

{ "type": "UNSUBSCRIBED", "stationId": 5 }

{ "type": "EVENT", "stationId": 5,
  "event": { "eventType": "NEW", "message": { ...StationChatMessageVO... } } }

{ "type": "EVENT", "stationId": 5,
  "event": { "eventType": "DELETE", "deletedMessageId": 12 } }

{ "type": "ERROR", "error": { "message": "已達每日聊天則數上限 (10 則)，請明日再試!" } }

{ "type": "ERROR", "fieldErrors": { "chatType": "請輸入留言類型!" } }
```

- `EVENT.event` 即現行 `StationChatEventVO`，序列化結果與現況一致（沿用 `@JsonInclude(NON_NULL)`）
- `ERROR` 保留現行兩種錯誤格式：一般錯誤為 `MessageVO`（對應 `error` 欄位）、表單驗證為欄位→訊息 Map（對應 `fieldErrors` 欄位），與現行 `@MessageExceptionHandler` 的兩種回傳型別對等

### 3.3 自訂 Close Code（4000–4999 為應用程式保留區段）

| Code | 語意              | 觸發時機                                 |
| ---- | ----------------- | ---------------------------------------- |
| 4401 | 未認證 / 認證失敗 | AUTH 驗證失敗；或未認證前發送其他 action |
| 4400 | 協定違規          | 無法解析的 action、非 JSON 訊息          |

> 對照現況：STOMP CONNECT 驗證失敗時 `StompAuthException` 導致連線終止，前端收到 ERROR frame 後斷線。改為原生後，等效行為是「回發一則 `ERROR` 訊息，隨即以 4401 關閉連線」，前端可從 close event 的 code 判斷是否需要引導重新登入。

---

## 四、重構方案

新的檔案佈局（`module/stationChat` 內）：

```
stationChat/
├── config/
│   └── WebSocketConfig.java              # 改寫：@EnableWebSocket 註冊 handler
├── handler/                              # 新增目錄
│   └── StationChatWebSocketHandler.java  # 取代 StationChatStompController
├── service/
│   ├── StationChatService.java           # 不動
│   ├── StationChatSessionRegistry.java   # 新增：訂閱登記表 + 廣播
│   └── WebSocketAuthService.java         # 取代 StompAuthChannelInterceptor
├── enums/
│   ├── ChatActionEnum.java               # 新增：inbound action
│   └── ChatEventTypeEnum.java            # 不動
├── model/
│   ├── dto/ChatCommandDTO.java           # 新增：inbound 訊息信封
│   └── vo/ChatServerMessageVO.java       # 新增：outbound 訊息信封
└── exceptions/
    └── WebSocketAuthException.java       # 取代 StompAuthException
```

### 步驟 1：改寫 `WebSocketConfig`

移除 `@EnableWebSocketMessageBroker` 與四個 override，改為：

```java
/**
 * 處理原生 WebSocket 的客製化配置。
 * 註冊車站聊天室的 WebSocket 進入點 /ws 並設定 CORS 與容器層限制。
 *
 * @author D5042101
 * @since 2026.07.19
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final StationChatWebSocketHandler stationChatWebSocketHandler;
    private final List<String> allowedOrigins;

    public WebSocketConfig(StationChatWebSocketHandler stationChatWebSocketHandler,
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.stationChatWebSocketHandler = stationChatWebSocketHandler;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * 註冊 WebSocket 連線的進入點 /ws 並設定 CORS。
     * 瀏覽器發起 HTTP 請求完成 Upgrade 後，直接以原生 WebSocket 收發自訂 JSON 訊息。
     *
     * @param registry WebSocket handler 註冊器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(stationChatWebSocketHandler, "/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }

    /**
     * 設定容器層的訊息大小上限與閒置逾時，防止異常大的 frame 佔用記憶體。
     *
     * @return ServletServerContainerFactoryBean
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(64 * 1024);
        container.setMaxSessionIdleTimeout(15 * 60 * 1000L);
        return container;
    }
}
```

> 路徑維持 `/ws`，`app.cors.allowed-origins` 設定沿用，前端連線 URL 不變。
>
> `pom.xml` 的 `spring-boot-starter-websocket` **保留**——原生 WebSocket 同樣由它提供；只是不再使用其中 spring-messaging 的 STOMP 部分。

### 步驟 2：新增 `StationChatSessionRegistry`（取代 Simple Broker）

集中管理「車站 → 訂閱 session」的對應，並提供廣播。這是取代 `enableSimpleBroker` + `SimpMessagingTemplate` 的核心元件：

```java
/**
 * 管理車站聊天室的 WebSocket 訂閱關係與廣播，取代 STOMP Simple Broker 的角色。
 * 以車站 id 為 key 維護訂閱 session 集合，所有集合操作皆為執行緒安全。
 *
 * @author D5042101
 * @since 2026.07.19
 */
@Component
public class StationChatSessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(StationChatSessionRegistry.class);

    /** 車站 id → 訂閱該車站的 session 集合 */
    private final Map<Integer, Set<WebSocketSession>> subscriptionsByStationId = new ConcurrentHashMap<>();

    /**
     * 將 session 登記為指定車站的訂閱者。
     *
     * @param stationId
     * @param session   已包裝為 ConcurrentWebSocketSessionDecorator 的 session
     */
    public void subscribe(Integer stationId, WebSocketSession session) {
        subscriptionsByStationId
                .computeIfAbsent(stationId, key -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    /**
     * 取消 session 對指定車站的訂閱。
     *
     * @param stationId
     * @param session
     */
    public void unsubscribe(Integer stationId, WebSocketSession session) {
        Set<WebSocketSession> sessions = subscriptionsByStationId.get(stationId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    /**
     * 移除 session 在所有車站的訂閱，供連線關閉時清理使用。
     *
     * @param session
     */
    public void removeSession(WebSocketSession session) {
        subscriptionsByStationId.values().forEach(sessions -> sessions.remove(session));
    }

    /**
     * 將訊息廣播給訂閱指定車站的所有 session。
     * 單一 session 發送失敗僅記錄警告並移除該 session，不影響其他訂閱者。
     *
     * @param stationId
     * @param payloadJson 已序列化的 JSON 字串
     */
    public void broadcast(Integer stationId, String payloadJson) {
        Set<WebSocketSession> sessions = subscriptionsByStationId.get(stationId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage(payloadJson);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                session.sendMessage(textMessage);
            } catch (IOException exception) {
                logger.warn("廣播車站聊天訊息失敗，移除 session，stationId: {}, sessionId: {}, 原因: {}",
                        stationId, session.getId(), exception.getMessage());
                sessions.remove(session);
            }
        }
    }
}
```

**並發安全的關鍵**：`WebSocketSession.sendMessage()` 在 Tomcat 上**不是執行緒安全的**——兩個使用者幾乎同時發言時，廣播會從兩條請求執行緒同時對同一條連線寫入，觸發 `IllegalStateException [TEXT_FULL_WRITING]`。STOMP 時代由 broker channel 的消費順序迴避了這個問題；改為原生後，必須在 `afterConnectionEstablished` 就把 session 包成 `ConcurrentWebSocketSessionDecorator`（見步驟 4），登記表與所有回寫一律持有包裝後的實例。**此點是本次重構最容易踩雷的地方，code review 需特別確認沒有任何裸 session 流入登記表。**

### 步驟 3：新增 `WebSocketAuthService`（取代 `StompAuthChannelInterceptor`）

JWT 驗證邏輯（有效性 → Redis 黑名單 → 載入使用者）**逐行搬移**，僅改變觸發時機與回傳方式：

```java
/**
 * 處理 WebSocket 連線後首則 AUTH 訊息的 JWT 驗證。
 * 比照 {@link JwtFilter} 的驗證流程，但每條連線僅於認證階段執行一次，
 * 驗證通過後將使用者身分存入 session attributes，供整個連線生命週期使用。
 *
 * @author D5042101
 * @since 2026.07.19
 */
@Service
public class WebSocketAuthService {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final RedisService redisService;

    // 建構子注入，略

    /**
     * 驗證 accessToken 並回傳對應使用者的 email。
     * 依序檢查 token 有效性、Redis 黑名單，與對應的使用者資訊。
     *
     * @param token 純 accessToken 字串
     * @return 已驗證使用者的 email
     * @throws WebSocketAuthException token 缺漏、無效、已列入黑名單或查無使用者
     */
    public String authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new WebSocketAuthException("缺少認證 Token，拒絕建立 WebSocket 連線!");
        }

        try {
            if (!jwtUtil.validateAccessToken(token)) {
                throw new WebSocketAuthException("token驗證失敗，拒絕建立 WebSocket 連線");
            }

            String jti = jwtUtil.getJtiFromToken(token);
            if (redisService.isAccessTokenJtiInBlacklist(jti)) {
                throw new WebSocketAuthException("token已失效，拒絕建立 WebSocket 連線");
            }

            String email = jwtUtil.getEmailFromAccessToken(token);
            userDetailsService.loadUserByUsername(email);

            return email;
        } catch (WebSocketAuthException error) {
            throw error;
        } catch (Exception error) {
            throw new WebSocketAuthException("Token 驗證失敗，拒絕建立 WebSocket 連線", error);
        }
    }
}
```

差異說明：

- 原本把 `UsernamePasswordAuthenticationToken` 塞進 `StompHeaderAccessor.setUser()`，讓 `@MessageMapping` 方法拿到 `Principal`。原生模式下沒有 STOMP session，改為把 **email 存入 `session.getAttributes()`**，handler 呼叫 Service 時直接傳遞——`StationChatService.sendMessage()` / `deleteMessage()` 的參數本來就是 `String email`，介面完全吻合，Service 不需任何修改。
- `loadUserByUsername()` 保留呼叫以維持「認證時即確認使用者存在」的現行行為（查無使用者會拋例外，被外層 catch 轉為認證失敗）。

同時新增 `WebSocketAuthException`（`extends RuntimeException`，Javadoc 修正為「WebSocket 認證失敗時拋出的例外」——現行 `StompAuthException` 的 Javadoc 誤寫為「找不到指定車站聊天留言」，一併淘汰）。不需在 `GlobalExceptionHandler` 註冊：此例外只在 WebSocket 訊息處理流程內流動，由 handler 自行轉為 4401 關閉，不會傳播到一般 HTTP 請求。

### 步驟 4：新增 `StationChatWebSocketHandler`（取代 `StationChatStompController`）

繼承 `TextWebSocketHandler`，是整個協定的路由中樞：

```java
/**
 * 處理車站聊天室原生 WebSocket 連線的核心 handler。
 * 負責認證、訂閱管理、留言發送與刪除的訊息路由，處理完成後廣播給訂閱該車站的所有使用者。
 * 可使用 Postman 的 WebSocket Request 或 wscat 直接測試，無法使用 Swagger 測試。
 *
 * @author D5042101
 * @since 2026.07.19
 */
@Component
public class StationChatWebSocketHandler extends TextWebSocketHandler {

    private final StationChatService stationChatService;
    private final StationChatSessionRegistry sessionRegistry;
    private final WebSocketAuthService webSocketAuthService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    private static final String ATTR_EMAIL = "authenticatedEmail";
    private static final String ATTR_DECORATED_SESSION = "decoratedSession";
    private static final int SEND_TIME_LIMIT_MS = 10 * 1000;
    private static final int SEND_BUFFER_SIZE_LIMIT = 512 * 1024;

    // 建構子注入，略
}
```

各生命週期方法的職責：

**`afterConnectionEstablished`**：將 session 包裝為 `ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_SIZE_LIMIT)` 並存入 attributes，後續一律使用包裝後實例。

**`handleTextMessage`**：反序列化為 `ChatCommandDTO` 後依 `action` 分流，整體包在 try-catch 中維持現行三段式錯誤處理：

```java
@Override
protected void handleTextMessage(WebSocketSession rawSession, TextMessage textMessage) throws IOException {
    WebSocketSession session = getDecoratedSession(rawSession);

    try {
        ChatCommandDTO command = objectMapper.readValue(textMessage.getPayload(), ChatCommandDTO.class);

        if (command.getAction() == ChatActionEnum.AUTH) {
            handleAuth(session, command);
            return;
        }

        // 未認證前拒絕所有其他指令，等同 STOMP 未 CONNECT 即 SEND 的防護
        String email = (String) session.getAttributes().get(ATTR_EMAIL);
        if (email == null) {
            sendError(session, new MessageVO("尚未完成認證，請先發送 AUTH 訊息!"));
            session.close(new CloseStatus(4401, "未認證"));
            return;
        }

        switch (command.getAction()) {
            case SUBSCRIBE -> handleSubscribe(session, command);
            case UNSUBSCRIBE -> handleUnsubscribe(session, command);
            case SEND_MESSAGE -> handleSendMessage(session, command, email);
            case DELETE_MESSAGE -> handleDeleteMessage(session, command, email);
            default -> sendError(session, new MessageVO("不支援的 action!"));
        }
    } catch (JsonProcessingException exception) {
        // 對應現行 MessageConversionException 的處理：不暴露 Jackson 底層錯誤訊息
        logger.error("WebSocket payload JSON 轉換失敗: {}", exception.getMessage());
        sendError(session, new MessageVO("請求格式錯誤，請確認欄位型別是否正確!"));
    } catch (RuntimeException exception) {
        // 對應現行 @MessageExceptionHandler 的兜底處理：業務例外訊息回發本人
        logger.error("WebSocket 訊息處理發生例外: {}", exception.getMessage());
        sendError(session, new MessageVO(exception.getMessage()));
    }
}
```

**`handleAuth`**：呼叫 `webSocketAuthService.authenticate()`，成功則存入 attributes 並回 `AUTH_SUCCESS`；捕捉 `WebSocketAuthException` 時回發 `ERROR` 後以 4401 關閉連線（對等現行 CONNECT 失敗即斷線的效果）。

**`handleSendMessage`**：對等現行 `sendMessage` 的 `@MessageMapping`：

```java
private void handleSendMessage(WebSocketSession session, ChatCommandDTO command, String email) throws IOException {
    SendMessageDTO sendMessageDTO = command.getPayload();

    // 手動觸發 Jakarta Validation，對等現行 @Valid @Payload 的效果
    Map<String, String> fieldErrors = validate(sendMessageDTO);
    if (!fieldErrors.isEmpty()) {
        sendFieldErrors(session, fieldErrors);
        return;
    }

    StationChatMessageVO messageVO = stationChatService.sendMessage(command.getStationId(), sendMessageDTO, email);

    broadcastEvent(command.getStationId(), new StationChatEventVO(ChatEventTypeEnum.NEW, messageVO, null));
}
```

其中 `validate()` 使用注入的 `jakarta.validation.Validator`（Spring Boot 已自動配置該 bean）：

```java
private Map<String, String> validate(SendMessageDTO sendMessageDTO) {
    Map<String, String> errors = new HashMap<>();

    if (sendMessageDTO == null) {
        errors.put("payload", "請輸入留言內容物件!");
        return errors;
    }

    Set<ConstraintViolation<SendMessageDTO>> violations = validator.validate(sendMessageDTO);
    violations.forEach(violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

    return errors;
}
```

回傳的欄位→訊息 Map 與現行 `handleValidationException` 組出的格式一致，前端錯誤顯示邏輯可沿用。

**`handleDeleteMessage`**：對等現行 `deleteMessage`，`stationId` 與 `messageId` 從 DTO 欄位取得（需補 null 檢查，因為原本由路徑變數保證存在），呼叫 Service 後廣播 `DELETE` 事件。

**`broadcastEvent`**：組裝 outbound 信封並委派登記表：

```java
private void broadcastEvent(Integer stationId, StationChatEventVO event) throws IOException {
    String payloadJson = objectMapper.writeValueAsString(ChatServerMessageVO.event(stationId, event));
    sessionRegistry.broadcast(stationId, payloadJson);
}
```

**`afterConnectionClosed` / `handleTransportError`**：呼叫 `sessionRegistry.removeSession()` 清理訂閱，記錄 debug / warn 日誌。此職責原由 Simple Broker 在 DISCONNECT 時自動處理，**遺漏會造成 session 洩漏，廣播迴圈越跑越慢**，驗收時需以「連線→訂閱→斷線→再廣播」路徑確認清理完整。

### 步驟 5：新增協定模型

**`ChatActionEnum`**（`enums/`）：`AUTH`、`SUBSCRIBE`、`UNSUBSCRIBE`、`SEND_MESSAGE`、`DELETE_MESSAGE`，Jackson 以列舉常數名稱反序列化（與 `ChatEventTypeEnum` 同慣例）。

**`ChatCommandDTO`**（`model/dto/`）：inbound 信封，欄位 `action`（`@NotNull`）、`token`、`stationId`、`messageId`、`payload`（型別 `SendMessageDTO`）。依協定不同 action 使用不同欄位子集，未使用欄位為 null。

**`ChatServerMessageVO`**（`model/vo/`）：outbound 信封，欄位 `type`、`stationId`、`event`（`StationChatEventVO`）、`error`（`MessageVO`）、`fieldErrors`（`Map<String, String>`），標註 `@JsonInclude(NON_NULL)` 使未使用欄位不輸出。提供靜態工廠方法 `event()`、`authSuccess()`、`subscribed()`、`error()`、`fieldErrors()` 集中組裝，避免 handler 內散落 new。

### 步驟 6：移除與收尾

| 動作 | 對象                                                                                                                             |
| ---- | -------------------------------------------------------------------------------------------------------------------------------- |
| 刪除 | `StompAuthChannelInterceptor`、`StationChatStompController`、`StompAuthException`                                                  |
| 改寫 | `WebSocketConfig`（步驟 1）                                                                                                        |
| 修正 | `GlobalExceptionHandler.java:262` 附近註解（指涉 `StationChatStompController` 之處改為 `StationChatWebSocketHandler`）             |
| 修正 | `StationChatService.sendMessage()` Javadoc（「供 STOMP 廣播使用」等字樣）、`StationChatEventVO` 與 `ChatEventTypeEnum` 的 Javadoc |
| 保留 | `pom.xml` 的 `spring-boot-starter-websocket`、`SecurityConstants` 的 `/ws/**`                                                      |

---

## 五、功能等效性驗收標準

以 Postman WebSocket Request（改為原生後即可直接使用，不再需要 STOMP 工具）逐項驗證：

1. **認證成功**：連線 `/ws` → 發送 `AUTH`（有效 token）→ 收到 `AUTH_SUCCESS`
2. **認證失敗**：無效 token、黑名單 token（登出後的 token）、缺 token 三種情況，皆收到 `ERROR` 後連線以 4401 關閉
3. **未認證防護**：連線後直接發送 `SEND_MESSAGE`，收到錯誤且連線關閉（對等現行未 CONNECT 即操作的拒絕行為）
4. **發送與廣播**：兩條已認證連線訂閱同一車站，A 發送文字留言後，A、B 皆收到 `EVENT`（`eventType=NEW`，`message` 內容與現行 `StationChatMessageVO` 一致，含旅程分享的 `travelTimeSeconds` 補值）
5. **隔離性**：訂閱車站 5 的連線不會收到車站 6 的事件
6. **刪除與廣播**：刪除留言後，訂閱者皆收到 `eventType=DELETE` 與 `deletedMessageId`
7. **權限**：非本人且非 ADMIN 刪除留言，僅操作者本人收到 `ERROR`（訊息「僅本人或管理員可以刪除此留言!」），無廣播
8. **表單驗證**：`SEND_MESSAGE` 缺 `chatType`，僅本人收到 `fieldErrors: { "chatType": "請輸入留言類型!" }`
9. **格式錯誤**：發送非法 JSON 或欄位型別錯誤（如 `chatType: 123`），僅本人收到「請求格式錯誤，請確認欄位型別是否正確!」
10. **業務例外**：每日則數上限、找不到車站、分享非本人旅程等 Service 例外，僅本人收到對應 `ERROR` 訊息（與現行 `/user/queue/errors` 收到的內容一致）
11. **清理**：斷線後重複廣播不拋例外、登記表無殘留（可暫時以 debug 日誌輸出集合大小驗證）
12. **並發寫入**：兩個 client 對同一訂閱車站快速連續發言，無 `TEXT_FULL_WRITING` 例外（驗證 decorator 生效）
13. `./mvnw clean package` 通過，全案無 `org.springframework.messaging.simp` 的殘留 import

---

## 六、實作順序與 commit 建議

由於新舊協定共用 `/ws` 路徑且前端必須同步改版，**採一次性切換**（single PR），不做並行雙協定的過渡期。PR 內以邏輯順序分 commit：

| 順序 | 項目                                                                                            | commit message                                           |
| ---- | ----------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| 1    | 協定模型（`ChatActionEnum`、`ChatCommandDTO`、`ChatServerMessageVO`、`WebSocketAuthException`） | `feat(stationChat): 新增原生 WebSocket 訊息協定模型`     |
| 2    | `WebSocketAuthService` + `StationChatSessionRegistry`                                           | `feat(stationChat): 新增 WebSocket 認證服務與訂閱登記表` |
| 3    | `StationChatWebSocketHandler` + `WebSocketConfig` 改寫 + 移除 STOMP 三檔 + Javadoc 收尾         | `refactor(stationChat): 以原生 WebSocket 連線取代 STOMP` |

---

## 七、前端協作事項

此重構為**破壞性變更**，前端需同步改版後一起上線：

- 移除 `@stomp/stompjs`（或相依的 STOMP client），改用瀏覽器原生 `WebSocket` API：`new WebSocket("ws://.../ws")`
- 連線建立後第一件事發送 `AUTH` 訊息；收到 `AUTH_SUCCESS` 後再發送 `SUBSCRIBE`
- 進入車站聊天室頁面時 `SUBSCRIBE`、離開時 `UNSUBSCRIBE`；斷線重連後需**自行重新走 AUTH → SUBSCRIBE 流程**（STOMP client 的自動重連與重新訂閱功能不復存在，需自行實作重連退避）
- 收訊息端從「訂閱 `/topic/station-chat/{id}` + `/user/queue/errors` 兩個目的地」改為「單一 `onmessage` 依 `type` 欄位分流」
- close event 的 `code === 4401` 代表認證問題，應引導使用者重新取得 token 或重新登入

---

## 八、附錄：設計決策 Q&A

**Q：為什麼 token 用首則訊息傳，而不是放在 handshake 的 query string？**

瀏覽器原生 `WebSocket` API 無法自訂 HTTP header（這正是 STOMP 把 token 放 CONNECT frame header 的原因），因此 handshake 階段只剩 query string 一途。但 query string 會被寫入 access log、proxy log 與瀏覽器歷史，把 accessToken 落在這些地方是明確的安全反模式。首則訊息認證把 token 留在 WebSocket frame 內（TLS 下不落地），且與現行「連線後才驗證」的時序完全一致，是小型專案的主流選擇。代價是存在「已連線但未認證」的短暫窗口，以「非 AUTH 指令一律拒絕並斷線」防護即可；若要更嚴格可加認證逾時（連線後 N 秒未 AUTH 即斷線），本次先不引入排程複雜度。

**Q：為什麼一定要 `ConcurrentWebSocketSessionDecorator`？**

Tomcat 的 WebSocket RemoteEndpoint 同一時間只允許一條執行緒寫入。STOMP 架構下所有 outbound 訊息經由 broker channel 序列化消費，天然避開競爭；改為原生後，「使用者 A 的請求執行緒」與「使用者 B 觸發的廣播執行緒」可能同時寫同一條連線，會直接拋 `IllegalStateException`。decorator 內部以佇列緩衝並保證單執行緒寫出，是 Spring 官方文件對此問題的標準解。

**Q：為什麼 `StationChatService` 完全不用改？**

現行 STOMP Controller 已把協定關注點（`Principal`、`@DestinationVariable`）在邊界拆解，Service 介面只收 `stationId`、DTO、`email` 等純業務參數。這次只是換掉邊界層，正好驗證了原本分層的正確性。日後若再換傳輸協定（如 SSE），Service 依然不動。

**Q：SUBSCRIBE 要不要驗證車站存在？**

現行 Simple Broker 對任意 topic 的訂閱一律接受，不驗證車站。為維持功能等效，本次 `handleSubscribe` 同樣不查資料庫（發送與刪除路徑已有車站驗證把關）。若未來要防止惡意訂閱大量無效 id 撐爆登記表，可加上 `metroService.existsStationById()` 檢查，屬後續強化項目。

**Q：需要心跳（heartbeat）嗎？**

現況的 Simple Broker 未配置 TaskScheduler，heartbeat 為 `0,0`（停用），因此「改為原生後沒有心跳」並未減損任何現有功能。連線存活由容器 idle timeout（步驟 1 設 15 分鐘）與前端重連機制管理。若日後發現行動網路下有殭屍連線問題，再以 `PING`/`PONG` action 或容器層 ping/pong 機制補強。
