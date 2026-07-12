# UML use case diagram 講述

## 開場

這是 lookGo 系統的 use case 圖。

## 參與者 (Actor)

- 系統的參與者有 5 個
  - 請先看圖的左邊，**訪客**是最基礎的使用者，**一般會員**繼承訪客權限、**管理員**再繼承一般會員。
  - 圖的右邊，則有 **TDX、DataTaipei** 這 2 個外部系統，提供北捷資料來源。

## 用例 (Use Case)

系統邊界內把用例分成 6 個區塊，並用顏色對應可操作的參與者

- **帳號認證**
  - 訪客可以進行**註冊、登入、忘記密碼重設**
  - 但**登出**則只有登入的會員能使用
- **臺北捷運資訊**：
  - 瀏覽、查詢所有資訊都開放讓訪客就能使用
  - 如果要更新或手動及時同步資料，只有管理員能操作。
- **會員**：
  - 一般會員可以改自己的個資、密碼。
  - 如果要**查詢所有會員、控制帳號啟用或停用**，必須要管理員才能操作。
- **車站書籤**：
  - 會員可以對特定車站書籤進行收藏、查詢、移除，也可以匯出 Excel
- **車站聊天室**：
  - 會員可以查詢指定車站的訊息、公告 + 也能即時聊天
  - 管理員則多出能管理公告、匯出當日聊天訊息的功能。
- **旅程規劃**：
  - 會員可以針對特定車站來規劃旅程，並且能事後修改、刪除，也可以匯出 Excel

## 記法的取捨（技術主管通常會問，主動交代）

1. **為何按功能分組、不用權限分層？** 功能分組回答「系統做什麼」，是給人看業務範圍的；權限我用繼承＋顏色承載，不必犧牲功能視角。
2. **為何整張圖沒有 include／extend？** 這兩個只描述「同一次執行內的行為相依」。這個系統的匯出、瀏覽等都是**可獨立發起的目標**，彼此只是前置條件（例如要先有書籤才能匯出），前置條件不畫箭頭——所以刻意一個都不加，避免誤導。
3. **排程為什麼不是 Actor？** 同步排程、清理排程是**系統內部元件**、不是外部角色，畫成 Actor 是常見錯誤；它們只在圖下方以**附註**呈現（捷運每週日同步、聊天每日清理）。

---

# code review 各模組邏輯講述

## 講述原則

- 順序 (若沒指定，則依 use case 分類由上往下)
- 每個用例解釋：
  - config → controller (含參數驗證) → service (含 exception) → dao (含 mapper)

## 全域機制

> 對回圖上的「繼承鏈與三色權限」——這是跨所有分類用例的共同機制。

- **檔案**
  - config：`core/config/SecurityConfig.java`、`module/user/config/AdminInitializer.java`
  - constants：`core/constants/SecurityConstants.java`
  - security：`core/security/JwtFilter.java`、`core/security/UserDetailsServiceImpl.java`
- **授權規則**
  - 公開層是 `SecurityConstants.API_PUBLIC_ALL` 白名單走 `permitAll`、會員層是 `anyRequest().authenticated()`
  - 管理層是 16 個 `@PreAuthorize("hasRole('ADMIN')")` 端點（捷運管理 4、TDX 同步 6、會員管理 2、聊天室管理 4）。
- **初始化（無狀態設計）**
  - `SecurityConfig` 關閉 CSRF、Session 設 `STATELESS`，身分完全靠 JWT。
  - `JwtFilter` 插在 `UsernamePasswordAuthenticationFilter` 之前（`SecurityConfig:78`），並用 `FilterRegistrationBean.setEnabled(false)` 防止被 Spring Boot 重複註冊成 Servlet Filter 執行兩次（`SecurityConfig:113–119`，易忽略）。
  - 密碼統一由 `BCryptPasswordEncoder` Bean 加密；`@EnableMethodSecurity` 啟用方法層 `@PreAuthorize`。
- **驗證時機（JwtFilter 六步鏈，每請求）**
  - 公開路徑跳過（`shouldNotFilter` + `AntPathMatcher`）→ Bearer token 存在性 → 簽章有效性 → Redis jti 黑名單檢查（`JwtFilter:117`）→ 解析 email 載入 UserDetails → 寫入 SecurityContext。
  - 角色檢查延後到方法層 `@PreAuthorize`。
- **實現（管理員帳號生成）**
  - `AdminInitializer`（ApplicationRunner）啟動時建立管理員——帳密來自 `@Value("${app.admin.*}")` 設定注入而非硬編碼，先 `existsByEmail` 防重複。
- **錯誤處理**
  - JwtFilter 驗證失敗不直接回錯，而是清掉 SecurityContext 放行，由授權階段觸發統一 401 `authenticationEntryPoint`（`SecurityConfig:70–77`）回 JSON。
  - 業務例外則由 `GlobalExceptionHandler` 統一轉 HTTP 狀態碼。

## 1. 帳號認證

- **檔案**
  - config：`core/config/SendGridConfig.java`
  - security：`core/security/JwtUtil.java`、`core/security/CookieUtil.java`
  - controller：`core/controller/AuthController.java`
  - service：`core/service/AuthService.java`
  - dao / mapper：`core/dao/AuthDAO.java`、`mappers/AuthMapper.xml`
- **驗證時機（核發雙 token）**
  - 註冊/登入成功「當下」核發雙 token——存取 token 放回應本體、刷新 token 由 `CookieUtil` 寫入 HttpOnly Cookie（防 XSS）。
  - 忘記密碼先驗 email＋手機一致，才核發 15 分鐘限時重設 token。
- **實現（登出即撤銷）**
  - 登出由 `LogoutResultHandler` 接手把 jti 寫入 Redis 黑名單，Controller 只是空殼。
- **錯誤處理**
  - 認證失敗不丟例外堆疊，`authenticationEntryPoint` 統一回 JSON 格式 401（`SecurityConfig:70–77`）。

## 2. ✨ 臺北捷運資訊

- **檔案**
  - 查詢：`MetroController.java`（前 8 個查詢端點）、`MetroService.java`、`MetroRouteGraphService.java`、`MetroForkBranchRouteGraphService.java`
  - 管理／同步：`MetroController.java`（4 個 `@PreAuthorize` 端點）、`MetroSyncController.java`（6 個同步端點）、`MetroSyncService.java`
  - 外部 API：`TDXApiClientConfig.java`、`DataTaipeiApiClientConfig.java`、`RailClientConfig.java`
  - dao / mapper：`MetroDAO.java`、`mappers/MetroMapper.xml`
  - 排程：`MetroSyncScheduler.java`
- **查詢類（訪客）**
  - 公開端點集中宣告在 `SecurityConstants.API_PUBLIC_ALL`，`SecurityConfig` 與 `JwtFilter` 共用同一份、改一處全域生效；`shouldNotFilter` 用 `AntPathMatcher` 在進 Filter 前就跳過驗證。
  - 亮點是 Service 層的圖演算法——`MetroRouteGraphService` 用 Dijkstra 算最短路徑（`:246–253` 鬆弛邏輯）、`MetroForkBranchRouteGraphService` 處理支線分岔，策略（最少轉乘/最短時間）由參數切換。
  - 查無車站丟 `StationNotFoundException` → 404。
- **同步外部資料（管理員／本分類深潛點）**
  - 唯一不服務使用者請求的功能，觸發來源有二——ADMIN 手動（6 端點皆 `@PreAuthorize`，AOP 在方法呼叫前驗角色，是登入後的第二道防線）＋ 每週日 23:00 排程（`MetroSyncScheduler`，cron `0 0 23 * * SUN`）。
  - 有依賴順序（先路線車站，才能同步票價/轉乘/行駛時間，Swagger 有註明）。
  - 每個同步方法掛 `@Transactional`（共 6 處）整批成功才提交、失敗整批回滾；批次寫入 `batchSize=150`（每筆 13 參數，避開 SQL Server 單次 2100 參數上限，`MetroSyncService:152` 註解）。
- **外部 API 防禦與雙來源合併**
  - TDX 走 OAuth2 client credentials，token 快取於 Redis、miss 才換發，收 429 退避 90 秒（`TDXApiClientConfig:46` 註解）。
  - 車站資料雙來源合併——TDX 給路線/車站/票價/行駛時間、DataTaipei 給設施，合併後才落庫，任一來源失敗即回滾（全有或全無）。

## 3. 會員

- **檔案**
  - controller：`module/user/controller/UserController.java`
  - service：`module/user/service/UserService.java`
  - dao / mapper：`module/user/dao/UserDAO.java`、`mappers/UserMapper.xml`
- **一般會員（改個資／密碼）**
  - 每支 API 第一步從 `SecurityContextHolder` 取當前登入者 email（`UserService:236`），完全不接受前端傳 userId——防越權的根本設計。
  - 改密碼採「先驗舊密碼、再 BCrypt 加密」兩段式；改生日順帶判斷會員等級，BASIC 自動升級 PREMIUM（升級是副作用、非獨立 API）。
  - 五個更新方法皆 `@Transactional`（`:107–202`），複合寫入不做一半；查無使用者 → 404、舊密碼錯誤 → 401。
- **管理員（會員管理／開放問題）**
  - `get-all-user`、`update-status` 兩端點皆 `@PreAuthorize`（`UserController:91、201`）；`update-status` 用 enum code 驗證狀態合法性，無效 code → 400。
  - **關鍵追問（講稿亮點）**：停權寫入 DB 後，該使用者未過期的存取 token 是否立即失效？系統已有現成機制（Redis jti 黑名單＋`JwtFilter:117` 每請求檢查），需確認 `updateStatus` 有沒有把停權者 token 加入黑名單——有機制不代表有串接。

## 4. 車站書籤

- **檔案**
  - controller：`module/stationBookmark/controller/StationBookmarkController.java`
  - service：`service/StationBookmarkService.java`
  - dao / mapper：`dao/StationBookmarkDAO.java`、`mappers/StationBookmarkMapper.xml`
- **驗證時機**
  - 新增前依序——車站存在 → 未重複收藏 → 未達會員等級上限。
  - 上限由 DAO 依會員等級查表（`StationBookmarkDAO:94`）、非硬編碼（與旅程規劃同一套查表款式）。
- **實現**
  - 軟刪除（更新 `is_deleted` 旗標），所有查詢 SQL 必帶「未刪除」過濾（Mapper XML 必查點）。
  - Excel 匯出用 Apache POI，範圍是全部有效書籤、不受列表分頁影響。
- **錯誤處理**
  - 重複收藏、超過上限各有專屬例外（`BookmarkLimitExceededException` 等）→ 400，查無 → 404。
- **文件一致性案例（提醒）**
  - 本模組兩端點（查全部、匯出 Excel）Swagger 曾誤標 403 但程式未限制 ADMIN，已修正。
  - 「文件標註不等於安全控制，安全只認 SecurityConfig 與 `@PreAuthorize`」。

## 5. ✨ 車站聊天室

- **檔案**
  - config：`config/WebSocketConfig.java`、`config/StompAuthChannelInterceptor.java`
  - controller：`controller/StationChatController.java`（查詢＋公告端點）、`controller/StationChatStompController.java`
  - service：`service/StationChatService.java`
  - dao / mapper：`dao/StationChatDAO.java`、`mappers/StationChatMapper.xml`
  - 排程：`StationChatMessageCleanupScheduler.java`
- **初始化（第二條驗證路徑）**
  - HTTP 的 `JwtFilter` 攔不到 WebSocket，因此自定義 `StompAuthChannelInterceptor` 註冊進 `WebSocketConfig`。
- **驗證時機（只在 CONNECT）**
  - 只在 STOMP CONNECT 驗證一次——缺 token、驗證失敗、在 Redis 黑名單三種情況都 `throw StompAuthException` 拒絕連線（`StompAuthChannelInterceptor:71–93`）。
  - 通過後 Principal 綁定整條連線，後續 SEND 不重驗。
- **實現（會員發言／管理員管理）**
  - 發言前檢查每日則數上限（依會員等級，`StationChatService:270`）；刪留言雙軌授權 `isOwner || isAdmin`（`:312–314`）；成功後 `SimpMessagingTemplate` 廣播給訂閱該站的所有人。
  - 管理員多 4 個 `@PreAuthorize` 端點（公告 create/update/delete + 匯出，`StationChatController:124、152、174、196`），公告同為軟刪除。
- **錯誤處理**
  - STOMP 無 HTTP 狀態碼，用 `@MessageExceptionHandler` + `@SendToUser("/queue/errors")` 私訊發送者。
  - JSON 轉換錯誤包裝成繁中訊息，避免洩漏 Jackson 內部細節。
- **資料生命週期（時序耦合）**
  - 清理排程每日 03:00 清空全站留言（`StationChatMessageCleanupScheduler`，cron `0 0 3 * * *`；整段 try-catch 記 log `:39–44`，單次失敗不影響下次），留言只活一天。
  - 管理員匯出 Excel 是「指定車站、不分頁的完整當日紀錄」，實質是資料歸檔的最後機會，兩功能有時序耦合。

## 6. 旅程規劃

- **檔案**
  - controller：`module/tripPlan/controller/TripPlanController.java`
  - service：`service/TripPlanService.java`
  - dao / mapper：`dao/TripPlanDAO.java`、`mappers/TripPlanMapper.xml`
- **Controller / DTO**
  - `@Valid` + Jakarta Validation 在入口擋參數錯誤；DTO 進、VO 出。
- **Service（擁有權檢查單一入口）**
  - 所有指定 id 的操作（改名、更新、刪除、匯出）第一步都呼叫同一私有方法 `getCurrentUserIdAndCheckOwnership()`（`TripPlanService:327`）——當前使用者一律取自 `SecurityContextHolder`、不信任前端傳的 id，不符擁有者丟 `TripPlanAccessDeniedException` → 403。
  - 擁有權檢查收斂成單一入口是本模組最值得講的設計。
- **業務限制查表化**
  - 新增前查會員等級數量上限（DAO 查表、非硬編碼常數），超過丟 `TripPlanLimitExceededException` → 400。
  - 票種只允許 1/4/5/7、無效值 Service 層擋下。
- **DAO / Mapper**
  - 軟刪除設計，所有 SELECT 都帶未刪除過濾條件。
- **錯誤處理**
  - 403（非本人）/ 404（查無）/ 400（參數與上限）三類分流，訊息含具體 id。
