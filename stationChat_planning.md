# stationChat 模組開發規劃整理（v2）

> 本版依實際資料表 DDL 更新：Trip Plan 分享改為 `trip_plan_id` FK 關聯 `user_trip_plans`（取代原先 JSON snapshot 存 `content` 的規劃），公告表 `created_by` 命名定案，並修正 DDL 語法錯誤與補充索引建議。

---

## 一、資料表修改

### 1. `station_chat_messages`（既有表，語法修正 + 設計確認）

```sql
CREATE TABLE [dbo].[station_chat_messages] (
    [id]           INT             NOT NULL IDENTITY(1, 1),
    [station_id]   INT             NOT NULL,
    [user_id]      INT             NOT NULL,
    [trip_plan_id] INT             NULL,           -- 旅程分享時才關聯
    [chat_type]    TINYINT         NOT NULL,        -- 1.手動輸入文字 2.旅程分享
    [content]      NVARCHAR(1000)  NULL,            -- 只在 chat_type=1 時存值
    [created_at]   DATETIME2(0)    NOT NULL,
    [deleted_at]   DATETIME2(0)    NULL,
    CONSTRAINT [PK_station_chat_messages] PRIMARY KEY ([id]),
    CONSTRAINT [FK_station_chat_messages_station_id]
        FOREIGN KEY ([station_id]) REFERENCES [dbo].[stations] ([id]),
    CONSTRAINT [FK_station_chat_messages_user_id]
        FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_station_chat_messages_trip_plan_id]
        FOREIGN KEY ([trip_plan_id]) REFERENCES [dbo].[user_trip_plans] ([id])
);
```

| 項目 | 說明 |
| ---- | ---- |

| 建議索引（新增） | `CREATE NONCLUSTERED INDEX [IX_station_chat_messages_station_created] ON [dbo].[station_chat_messages] ([station_id], [created_at] DESC) WHERE [deleted_at] IS NULL;`（分頁查詢基本必要索引） |

### 2. `station_chat_announcements`（新增表）

```sql
CREATE TABLE [dbo].[station_chat_announcements] (
    [id]          INT             NOT NULL IDENTITY(1, 1),
    [station_id]  INT             NOT NULL,
    [content]     NVARCHAR(1000)  NOT NULL,
    [created_by]  INT             NOT NULL,  -- 建立者 user_id（不命名為 admin_id，見下方說明）
    [created_at]  DATETIME2(0)    NOT NULL,
    [updated_at]  DATETIME2(0)    NOT NULL,
    [deleted_at]  DATETIME2(0)    NULL,
    CONSTRAINT [PK_station_chat_announcements] PRIMARY KEY ([id]),
    CONSTRAINT [FK_station_chat_announcements_station_id]
        FOREIGN KEY ([station_id]) REFERENCES [dbo].[stations] ([id]),
    CONSTRAINT [FK_station_chat_announcements_created_by]
        FOREIGN KEY ([created_by]) REFERENCES [dbo].[users] ([id])
);
```

| 項目 | 說明 |
| ---- | ---- |

| 建議索引（新增） | `CREATE NONCLUSTERED INDEX [IX_station_chat_announcements_station_created] ON [dbo].[station_chat_announcements] ([station_id], [created_at] DESC) WHERE [deleted_at] IS NULL;` |

### 3. 邊界情境提醒（非 DDL，設計時需留意）

- `user_trip_plans` 為軟刪除；`trip_plan_id` FK 本身不會因軟刪除而失效（資料列仍存在）。若查詢時比照慣例加上 `deleted_at IS NULL` 過濾，會查不到已被刪除的旅程規劃內容，需在 VO／前端明確處理「該旅程規劃已被刪除」的 fallback 顯示，避免整批訊息載入因單筆例外而中斷。

---

## 二、Java 程式碼開發

### (一) Config

| 項目                                        | 內容                                                                                                                                                                                                                                                                                                   |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 依賴套件                                    | pom.xml 新增 `spring-boot-starter-websocket`（相容現有 Spring Boot 5 stack，單機部署用內建 `SimpleBroker`，不需外部 broker 如 RabbitMQ）                                                                                                                                                               |
| `module/stationChat/config/WebSocketConfig` | 實作 `WebSocketMessageBrokerConfigurer`：<br>- `registerStompEndpoints("/ws")`（`setAllowedOriginPatterns` 比照現有 CORS 白名單）<br>- `enableSimpleBroker("/topic")` + `setApplicationDestinationPrefixes("/app")`                                                                                    |
| 訊息流向                                    | Client → Server：`/app/station-chat/{stationId}/send`、`/app/station-chat/{stationId}/delete/{messageId}`<br>Server → Client：廣播至 `/topic/station-chat/{stationId}`                                                                                                                                 |
| 身分驗證（`ChannelInterceptor`）            | STOMP handshake 完成 upgrade 後不會再走 `JwtFilter`，需自寫 `ChannelInterceptor` 攔截 `StompCommand.CONNECT`，從 CONNECT frame 自訂 header 取出 Bearer token，重用既有 `JwtUtil` 驗證並設定 `Principal`；`/ws/**` 加入 `API_PUBLIC_ALL`（handshake 本身放行，驗證交給 CONNECT 攔截，失敗直接拒絕連線） |
| 例外處理機制                                | `GlobalExceptionHandler` 僅適用 HTTP MVC；STOMP 場景需另用 `@MessageExceptionHandler`，將錯誤（如每日上限例外、Trip Plan 驗證例外）透過 `/user/queue/errors` 送回發送者本人                                                                                                                            |

---

## 三、REST API 設計

### 1、整體待確認清單

| 編號 | 待確認事項                                                                                                                                   |
| ---- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| 1    | `deleteMessage` 目前僅本人可刪；是否改成也允許 ADMIN 代刪任何 user 的違規訊息？                                                              |
| 2    | `sendMessage` 每日上限的「今日」判斷基準：是否以 UTC 00:00 為界（配合現有 `LocalDateTime.now(ZoneOffset.UTC)` 慣例），還是用台北時區 00:00？ |
| 3    | 分享 Trip Plan 時，`trip_plan_id` 是否需檢查該筆規劃的擁有者必須等於發送者本人？（避免分享他人旅程規劃，建議需要，見下方 API 6 設計）        |
| 4    | 既有 scaffold 路徑 `/api/station-chat` 需改為 `/api/v1/station-chat` 才符合規範                                                              |
| 5    | 已分享的 `trip_plan_id` 若原始 `user_trip_plans` 後續被軟刪除，畫面應如何顯示（fallback 文案），需與前端確認                                 |

### 2、各 API 開發順序與分層對應

> 開發順序：先靜態查詢（車站清單沿用既有 API）→ 訊息查詢 → 公告 CRUD → WebSocket 訊息發送／刪除（依賴前面公用元件）。

#### API 1：POST /get-message-by-station-id（依 stationId 分頁取得歷史訊息）

| 分層              | 對應內容                                                                                                                                                                                                                                                                                                                                                                               |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| (1) Model 層      | VO：`StationChatMessageVO(id, username, chatType, content, tripPlanId, fromStationName, toStationName, fareType, farePrice, transferCount, createdAt)`（`content` 系列欄位對應 `chat_type=1`，`tripPlanId` 起訖站/票價系列欄位對應 `chat_type=2`）；包裝：`PaginatedVO<StationChatMessageVO>`；DTO：`GetMessageByStationIdDTO(stationId, page, size)`；Enum：`ChatTypeEnum(TEXT=1, TRIP_PLAN=2)` |
| (2) 資料存取層    | `StationChatDAO.getMessagesByStationIdPaginated(stationId, offset, size)`：`WHERE station_id=? AND deleted_at IS NULL ORDER BY created_at DESC`，join `users` 取 `username`，LEFT JOIN `user_trip_plans`（含起訖 `stations`）取旅程分享內容；`StationChatDAO.countMessagesByStationId(stationId)`；對應 `StationChatMapper.xml`                                                        |
| (3) 業務邏輯層    | `StationChatService.getMessages(stationId, page, size)`：呼叫 `metroService.existsStationById(stationId)`，不存在拋 `StationNotFoundException`；若關聯的 `user_trip_plans` 已軟刪除，組裝 VO 時填入 fallback 顯示值而非中斷整批查詢；`GlobalExceptionHandler` 攔截 `StationNotFoundException` 回傳 404                                                                                 |
| (4) Controller 層 | `StationChatController` `POST /api/v1/station-chat/get-message-by-station-id`，權限：已驗證 user                                                                                                                                                                                                                                                                                        |

#### API 2：POST /get-announcement-by-station-id（依 stationId 取得公告列表）

| 分層              | 對應內容                                                                                                                                                                                                                                                                    |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| (1) Model 層      | VO：`StationChatAnnouncementVO(id, stationId, content, createdByUsername, createdAt, updatedAt)`；包裝：`List<StationChatAnnouncementVO>`；DTO：`GetAnnouncementByStationIdDTO(stationId)`                                                                                  |
| (2) 資料存取層    | `StationChatDAO.getAnnouncementsByStationId(stationId)`：`WHERE station_id=? AND deleted_at IS NULL ORDER BY created_at DESC`，join `users` 以 `created_by` 取 `createdByUsername`；對應 `StationChatMapper.xml`                                                             |
| (3) 業務邏輯層    | `StationChatService.getAnnouncements(stationId)`：`metroService.existsStationById(stationId)` 不存在拋 `StationNotFoundException`                                                                                                                                           |
| (4) Controller 層 | `StationChatController` `POST /api/v1/station-chat/get-announcement-by-station-id`，權限：已驗證 user                                                                                                                                                                        |

#### API 3：POST /create-announcement（新增公告）

| 分層              | 對應內容                                                                                                                                                                                                                                                                                                 |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| (1) Model 層      | Entity：`StationChatAnnouncement(id, stationId, content, createdBy, createdAt, updatedAt, deletedAt)`；DTO：`CreateAnnouncementDTO(stationId, content)`；VO：`MessageVO(message)`                                                                                                                        |
| (2) 資料存取層    | `UserDAO.getByEmail(email)`；`StationChatDAO.insertAnnouncement(entity)`：新增列，欄位 `station_id, content, created_by=user.getId(), created_at=now, updated_at=now`；對應 `UserMapper.xml`、`StationChatMapper.xml`                                                                                    |
| (3) 業務邏輯層    | `StationChatService.createAnnouncement(dto, email)`：<br>1. `userDAO.getByEmail(email)` 找不到拋 `UserNotFoundException`（防禦性）<br>2. `metroService.existsStationById(stationId)` 不存在拋 `StationNotFoundException`<br>3. 無唯一性檢查（同車站可多筆公告）<br>`GlobalExceptionHandler` 攔截上述例外 |
| (4) Controller 層 | `StationChatController` `POST /api/v1/station-chat/create-announcement`，`@PreAuthorize("hasRole('ADMIN')")`                                                                                                                                                                                             |

#### API 4：POST /update-announcement（編輯公告）

| 分層              | 對應內容                                                                                                                                                                                         |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| (1) Model 層      | DTO：`UpdateAnnouncementDTO(announcementId, content)`；VO：`MessageVO(message)`                                                                                                                  |
| (2) 資料存取層    | `StationChatDAO.getAnnouncementById(announcementId)`；`StationChatDAO.updateAnnouncementContentById(id, content, updatedAt=now)`：更動欄位 `content`、`updated_at`；對應 `StationChatMapper.xml` |
| (3) 業務邏輯層    | `StationChatService.updateAnnouncement(dto)`：查無此筆或 `deleted_at IS NOT NULL` 拋 `StationChatNotFoundException`；`GlobalExceptionHandler` 攔截回傳 404                                       |
| (4) Controller 層 | `StationChatController` `POST /api/v1/station-chat/update-announcement`，`@PreAuthorize("hasRole('ADMIN')")`                                                                                     |

#### API 5：POST /delete-announcement（軟刪除公告）

| 分層              | 對應內容                                                                                                                                                                  |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| (1) Model 層      | DTO：`DeleteAnnouncementDTO(announcementId)`；VO：`MessageVO(message)`                                                                                                    |
| (2) 資料存取層    | `StationChatDAO.getAnnouncementById(announcementId)`；`StationChatDAO.softDeleteAnnouncementById(id, deletedAt=now)`：更動欄位 `deleted_at`；對應 `StationChatMapper.xml` |
| (3) 業務邏輯層    | `StationChatService.deleteAnnouncement(announcementId)`：查無此筆或已刪除拋 `StationChatNotFoundException`                                                                |
| (4) Controller 層 | `StationChatController` `POST /api/v1/station-chat/delete-announcement`，`@PreAuthorize("hasRole('ADMIN')")`                                                              |

#### API 6：STOMP `/app/station-chat/{stationId}/send`（發送訊息，含一般文字／Trip Plan 分享）

| 分層              | 對應內容                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| (1) Model 層      | Entity：`StationChatMessage(id, stationId, userId, tripPlanId, chatType, content, createdAt, deletedAt)`；DTO：`SendMessageDTO(chatType, content, tripPlanId)` — `chatType` 加 `@Min(1) @Max(2)`，`content`／`tripPlanId` 依 `chatType` 擇一必填（Service 層驗證，非 Bean Validation 條件式驗證，避免過度設計）；VO：`StationChatMessageVO(id, username, chatType, content, tripPlanId, fromStationName, toStationName, fareType, farePrice, transferCount, createdAt)`、`ChatEventVO(eventType, payload)`；Enum：`ChatTypeEnum(TEXT=1, TRIP_PLAN=2)`、`ChatEventTypeEnum(NEW, DELETE)`                                                                                                                                                                                                                                                          |
| (2) 資料存取層    | `UserDAO.getByEmail(email)`；`TripPlanDAO.getTripPlanById(tripPlanId)`（`chat_type=2` 時使用，含起訖站資訊）；`StationChatDAO.getMaxDailyChatsByUserId(userId)`（join `users` → `membership_tiers` 取 `max_daily_chats`）；`StationChatDAO.countTodayMessagesByUserId(userId, todayStart)`（`created_at >= ? AND deleted_at IS NULL`）；`StationChatDAO.insertMessage(entity)`（MyBatis `useGeneratedKeys` 回填 `id`）；對應 `UserMapper.xml`、`TripPlanMapper.xml`、`StationChatMapper.xml`                                                                                                                                                                                                                                                                                                                                                     |
| (3) 業務邏輯層    | `StationChatService.sendMessage(stationId, dto, email)`：<br>1. `userDAO.getByEmail(email)` 找不到拋 `UserNotFoundException`<br>2. `metroService.existsStationById(stationId)` 不存在拋 `StationNotFoundException`<br>3. `chatType==1` → `content` 必填、`tripPlanId` 必為 `null`；`chatType==2` → `tripPlanId` 必填、`content` 必為 `null`，否則拋 `IllegalArgumentException`<br>4. `chatType==2` 時：`tripPlanDAO.getTripPlanById(tripPlanId)` 不存在或已軟刪除拋 `TripPlanNotFoundException`；擁有權檢查 `tripPlan.getUserId().equals(user.getId())` 為 `false` 拋 `TripPlanAccessDeniedException`（禁止分享他人旅程規劃）<br>5. 每日上限檢查：今日則數 ≥ 上限拋 `ChatDailyLimitExceededException`<br>例外處理：非 HTTP MVC 場景，改用 `@MessageExceptionHandler` 透過 `/user/queue/errors` 回傳發送者本人（不沿用 `GlobalExceptionHandler`） |
| (4) Controller 層 | `StationChatStompController`（`@MessageMapping`）：`@MessageMapping("/station-chat/{stationId}/send")`，處理完成後呼叫 `simpMessagingTemplate.convertAndSend("/topic/station-chat/"+stationId, new ChatEventVO("NEW", vo))`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |

#### API 7：STOMP `/app/station-chat/{stationId}/delete/{messageId}`（刪除自己的訊息）

| 分層              | 對應內容                                                                                                                                                                                                                                                                                                                                                                 |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| (1) Model 層      | 沿用 Entity：`StationChatMessage`；VO：`ChatEventVO(eventType="DELETE", messageId)`                                                                                                                                                                                                                                                                                      |
| (2) 資料存取層    | `UserDAO.getByEmail(email)`；`StationChatDAO.getMessageById(messageId)`；`StationChatDAO.softDeleteMessageById(messageId, deletedAt=now)`：更動欄位 `deleted_at`；對應 `UserMapper.xml`、`StationChatMapper.xml`                                                                                                                                                         |
| (3) 業務邏輯層    | `StationChatService.deleteMessage(messageId, email)`：<br>1. `userDAO.getByEmail(email)` 找不到拋 `UserNotFoundException`<br>2. 訊息不存在或已刪除拋 `StationChatNotFoundException`<br>3. 擁有權檢查：非本人拋 `ChatMessageAccessDeniedException`（僅本人可刪，ADMIN 亦不可代刪，待確認事項 1）<br>例外處理：同樣經 `@MessageExceptionHandler` 回傳 `/user/queue/errors` |
| (4) Controller 層 | `StationChatStompController`：`@MessageMapping("/station-chat/{stationId}/delete/{messageId}")`，處理完成後呼叫 `simpMessagingTemplate.convertAndSend("/topic/station-chat/"+stationId, new ChatEventVO("DELETE", messageId))`                                                                                                                                           |

---

### 附註一：REST API 權限總覽

| API | 路徑 | 權限 |
| --- | --- | --- |
| 取得訊息 | `POST /api/v1/station-chat/get-message-by-station-id` | 已驗證 user |
| 取得公告 | `POST /api/v1/station-chat/get-announcement-by-station-id` | 已驗證 user |
| 新增公告 | `POST /api/v1/station-chat/create-announcement` | ADMIN only |
| 編輯公告 | `POST /api/v1/station-chat/update-announcement` | ADMIN only |
| 刪除公告 | `POST /api/v1/station-chat/delete-announcement` | ADMIN only |

車站下拉選單沿用既有 `metro/get-all-station`，不需新增 API。

### 附註二：本版與前版的主要差異

| 差異點                      | 前版規劃                                             | 本版更正                                                                                                     |
| --------------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| Trip Plan 分享儲存方式      | `content` 存 JSON snapshot，需擴大為 `NVARCHAR(MAX)` | `trip_plan_id` FK 關聯 `user_trip_plans`，`content` 維持 `NVARCHAR(1000)`                                    |
| `sendMessage` 驗證邏輯      | `ObjectMapper.readTree(content)` 驗證 JSON 格式      | 驗證 `trip_plan_id` 存在性 + 擁有權檢查（新增 `TripPlanNotFoundException`、`TripPlanAccessDeniedException`） |
| `station_chat_messages` DDL | —                                                    | 補上遺漏逗號的語法錯誤                                                                                       |
| 公告建立者欄位命名          | 討論中                                               | 定案為 `created_by`，理由為命名應反映關聯本質（FK 指向 `users`）而非目前的授權規則                           |
| 索引設計                    | 未提及                                               | 新增 `station_chat_messages`、`station_chat_announcements` 的分頁查詢複合索引建議                            |
