# 0. 全域機制

## 3. 程式審查修改建議

整體大致遵守專案規範（建構子注入、SLF4J、白名單單一來源），以下為待修改處，按重要性排序：

| #   | 位置                                                             | 問題                                                                                                                         | 修改建議                                                                        |
| --- | ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| 1   | `AdminInitializer:30-40`                                         | 使用欄位層級 `@Value` 注入，與 `SecurityConfig:52-57` 以建構子參數接 `@Value` 的寫法不一致，且欄位無法宣告 `final`           | 改為建構子參數注入，統一風格                                                    |
| 2   | `AdminInitializer` 全類                                          | 完全沒有日誌——建立管理員成功或已存在略過都無紀錄，與其他檔案密集的 debug/warn log 風格落差大                                 | `run()` 內補 `logger.info`（建立／略過各一）                                    |
| 3   | `AdminInitializer:42、47-48`、`UserDetailsServiceImpl:31、35-36` | 建構子與公開方法缺 Javadoc，違反「所有公開方法必須加 Javadoc（含 `@param`、`@return`）」規範                                 | 補齊 Javadoc                                                                    |
| 4   | `JwtFilter:45-52`                                                | 建構子 Javadoc 只列了 `jwtUtil`、`userDetailsService`，漏列 `redisService` 參數                                              | 補上 `@param redisService`                                                      |
| 5   | `SecurityConstants:11`                                           | `public static final String[]` 陣列內容在執行期仍可被改寫，且類別無私有建構子                                                | 改用 `List.of(...)` 不可變集合，或保留陣列但加 `private SecurityConstants() {}` |
| 6   | `UserDetailsServiceImpl:46`                                      | 停用帳號拋 `UsernameNotFoundException`，語意上 Spring Security 內建的 `DisabledException` 更精確（現行功能正確，僅語意問題） | 視情況改用 `DisabledException`                                                  |

# 1. 帳號認證

## 3. 程式審查修改建議

Controller／Service／Mapper 大致遵守規範（建構子注入、SLF4J、DTO 進 VO 出、`@Valid` 驗證、Swagger 標註完整、DTO `toString` 皆遮罩密碼），以下為待修改處，按重要性排序：

| #   | 位置                                                                        | 問題                                                                                                                                                                                                                                  | 修改建議                                                                                                                   |
| --- | --------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| 1   | `LogoutResultHandler:62` vs `AuthService:233`                               | **【Bug】Redis key 不一致**：儲存刷新 token 白名單用 `userId` 當 key，登出刪除卻傳 `email`，刪除永遠落空——登出後刷新 token 在 TTL 內仍可透過 `/refresh-tokens` 換發全新雙 token，「登出即撤銷」只對存取 token 生效                    | 登出時改以 userId 刪除（可從刷新 token 解析 email 後查 DB 取 id，或改統一以 email 為 key），並補整合測試驗證登出後刷新失效 |
| 2   | `SendGridConfig.java` 全檔                                                  | 整個檔案（含 package 宣告）被註解掉，是死碼卻仍列在檔案清單中                                                                                                                                                                         | 恢復啟用或直接刪除檔案，並同步更新文件                                                                                     |
| 3   | `AuthService:56-57`                                                         | `frontendBaseUrl` 用欄位式 `@Value` 注入且全類無任何地方使用——死碼＋與建構子注入風格不一致                                                                                                                                            | 直接移除；若未來要用再以建構子參數注入                                                                                     |
| 4   | `AuthController:77-84、100-107` → `AuthService:87、128`                     | `signup()`／`login()` 把 `HttpServletResponse` 傳進 Service 但 Service 完全沒使用（Cookie 由 Controller 加）——Servlet API 洩漏進業務層，違反分層原則                                                                                  | 移除 Service 方法的 `HttpServletResponse` 參數                                                                             |
| 5   | `AuthService:229`                                                           | `generateTokens` 硬編碼 `Set.of("ROLE_USER")`，管理員登入時 token 的 roles claim 也標 ROLE_USER；subject 為每次隨機的 `UUID.randomUUID()`，無語意。功能未壞（`JwtFilter` 每請求從 DB 重載角色、不信 claim），但 claim 與事實不符      | roles claim 改帶使用者實際角色；subject 改用穩定識別（如 userId）                                                          |
| 6   | `AuthController:181`、`AuthController:132`、`AuthService:198`、`AuthDAO:20` | Javadoc 與程式脫節：logout 註解提到不存在的 `JwtLogoutSuccessHandler`（實名 `LogoutResultHandler`）；Controller 與 Service 的 `resetPassword` 殘留已移除的 `@param token`；`AuthDAO.createUser` 寫 `@param signupDTO` 但參數是 `user` | 修正 Javadoc 使其與程式一致                                                                                                |
| 7   | `AuthDAO` 全檔、`RedisService` 全檔                                         | `AuthDAO` 用 8 格縮排（全案 4 格）、`java.time.LocalDateTime` 未 import 用全限定名；`RedisService` 全類無 Javadoc、2 格縮排、TDX 區塊上方誤貼 `// Reset Password Token` 註解                                                          | 統一 4 格縮排、補 import 與 Javadoc、修正誤貼註解                                                                          |

# 2. 臺北捷運資訊

## 3. 程式審查修改建議

| 位置                                                                       | 問題                                                                                                                                                                                              | 修改建議                                                                                                                                                          |
| -------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `MetroSyncService.java:403`（`syncAllStationFare`，其餘 5 個同步方法同型） | **`@Transactional` 包住外部 API 呼召**：票價同步在交易內含 60 秒初始等待＋每頁 20 秒 `Thread.sleep` 的分頁請求，整段期間持有資料庫連線與交易，可能佔用連線池長達數分鐘，外部 API 逾時也會拖垮交易 | 改為「先抓後寫」：外部 API 抓取移出交易範圍，僅資料庫寫入段落標 `@Transactional`（拆出 writer 方法經 Spring 代理呼叫，或改用 `TransactionTemplate` 包住寫入區塊） |
| `RailClientConfig.java:70-91`                                              | 走 proxy 時信任全部憑證（trust-all SSL）且無環境防護，僅靠 Javadoc 提醒勿用於生產；`generateSslContext` 失敗時 `new RuntimeException("SSL 初始化失敗!")` 未附原始例外，遺失錯誤根因               | 以 `@Profile("dev")` 或設定旗標限制僅開發環境啟用並於啟動時記 warn；`RuntimeException` 建構子帶入 cause                                                           |
| `TDXApiClientConfig.java` / `DataTaipeiApiClientConfig.java`               | 類別後綴 `Config` 但實為 `@Component` API 客戶端（檔內註解也自稱「Client 層」），放在 config 套件易誤導；`TDXApiClientConfig` 的 TDX 憑證以欄位 `@Value` 注入，違反專案建構子注入規範             | 更名為 `TdxApiClient`、`DataTaipeiApiClient` 並移至 `client/` 套件；`@Value` 改由建構子參數注入（同 `SecurityConfig` 的做法）                                     |
| `MetroController.java`、`MetroService.java`、`MetroDAO.java`               | 縮排為 8 空格，與同模組的 Sync 系列檔案及專案其餘檔案的 4 空格不一致                                                                                                                              | 統一為 4 空格，並以 formatter 設定（如 `.editorconfig`）固定                                                                                                      |
| `MetroService.java:58-66`、`MetroSyncService.java:525-531`                 | `MetroService` 建構子 Javadoc 缺 `metroRouteGraphService` 參數；`toLineEntity` Javadoc 寫 `@param vo`、`@param now`，實際參數名為 `tdxLineVO`、`updatedAt`                                        | 補齊並同步 Javadoc 參數名與實際簽章                                                                                                                               |
| `MetroRouteGraphService.java:229-257`                                      | Dijkstra 的 `PriorityQueue` 以 `Object[]` 儲存 (cost, code) 並在迴圈內強制轉型，型別不安全且可讀性差                                                                                              | 定義 `private record NodeCost(int cost, String stationCode)` 取代 `Object[]`                                                                                      |
| `MetroController.java`（8 個公開查詢端點）                                 | 公開端點（列於 `API_PUBLIC_ALL`、實際 permitAll）的 Swagger 仍標註 401 回應，文件與實際安全行為不符（同車站書籤模組曾修正的文件不一致問題）                                                       | 移除公開端點的 401 `@ApiResponse` 標註                                                                                                                            |
| `MetroService.java:106-113、202-209`                                       | `getAnyStationCodeByStationId` 每次呼叫撈全表 `lines_stations` 再於記憶體過濾；`getStationByCode` 先 `existsByStationCode` 再查詳細資料，兩次資料庫往返                                           | 前者改為 DAO 帶條件查詢；後者直接查詢後判 null 拋 `StationNotFoundException`，省一次往返                                                                          |
| `MetroSyncService.java:58`                                                 | 常數 `DATA_TAIPEI_STATION_DATASET_id` 混用大小寫，違反 UPPER_SNAKE_CASE 規範                                                                                                                      | 改名 `DATA_TAIPEI_STATION_DATASET_ID`                                                                                                                             |

# 3. 會員

### 1.2 講稿「關鍵追問」的答案（停權後 token 是否立即失效）

review-script.md 留下的開放問題「`updateStatus` 有沒有把停權者 token 加入黑名單？」——結論是**沒有進黑名單，但實際風險已封閉**：

1. `UserService:222-225` **已串接** `redisService.deleteRefreshTokenJti(userId)`，且 key 用 userId、與儲存端一致（對比登出流程誤傳 email 的既知 bug，這裡是寫對的），刷新換發路徑封死。
2. 未過期的存取 token 確實**沒有**寫入 `blacklist:access_token:{jti}`——本來也做不到，因為系統沒有「userId → 已核發存取 token jti」的反查表，停權當下無從得知對方手上的 jti。
3. 真正的守門員是 `UserDetailsServiceImpl:44-47`：`JwtFilter` 每請求都重新從 DB 載入使用者，`status = DISABLED` 直接拋例外，存取 token 等同下一請求立即失效。**停權即時生效靠的是「每請求 DB 重載」而非黑名單**，這是無狀態 JWT 架構下用一次 DB 查詢換即時撤銷能力的取捨。

# 4. 車站書籤

### 1.2 盲點（本模組的關鍵發現）

1. **【水平越權】`deleteBookmark` 沒有擁有權檢查**（`StationBookmarkService:156-166`）：流程只有「書籤存在且未刪除 → 直接軟刪除」，從頭到尾沒有比對 `bookmark.getUserId()` 與當前登入者——任何會員拿連號整數 id 列舉就能刪光他人書籤。三個旁證顯示這是漏寫而非設計：同模組的單筆查詢有綁 `user_id = 自己`（`Mapper:78`）、刪除的 Swagger 標了 403 但程式沒有任何 403 的產生來源、旅程規劃模組同型操作有 `getCurrentUserIdAndCheckOwnership()` 單一入口把關。本模組最高優先修正項。
2. **【個資外洩疑慮】列表與匯出回傳全站會員個資**：`get-all-bookmark-paginated` 與 `get-excel` 不以 userId 過濾，任何一般會員都能翻頁撈到**所有使用者的 username 與 email**（VO 與 Excel 都含這兩欄）。文件面已處理（拿掉誤標的 403），但資料面的問題還在——「文件標註不等於安全控制」的另一面是：文件改對了，資料範圍的決策仍需重審。
3. **【併發縫隙】全 Service 無 `@Transactional`**：與 user 模組「五個寫入方法皆掛」的款式不一致；`createBookmark` 的「三道檢查 → insert」存在 TOCTOU 競態，同帳號併發請求可繞過重複收藏與數量上限檢查。交易本身擋不住這個（需要 DB 層防線），但至少該與全案寫入款式對齊。

# 5. 車站聊天室

### 1.2 盲點（審查發現）

1. **長連線不重驗——停權、登出、token 過期都踢不掉已連線的使用者（最高優先）**。驗證只在 CONNECT 做一次，之後 SEND 不再檢查 token 有效期、Redis 黑名單、帳號狀態。這直接回打會員模組 UC_D 的結論：停權會清掉 refresh token、`UserDetailsServiceImpl` 會擋下一次 HTTP 請求，**但對已建立的 WebSocket 連線完全無效**——被停權的會員只要不斷線就能繼續發言。`sendMessage` 每次都會從 DB 重載 User（`StationChatService:232-233`），卻只檢查存在性、不檢查 `status`，補一行 DISABLED 檢查即可堵住；更完整的做法是停權時透過 session registry 強制斷線。
2. **通用 `@MessageExceptionHandler` 直接回傳 `exception.getMessage()`**（`StationChatStompController:135-141`）。業務例外沒問題，但 SQL 例外、NPE 等非預期錯誤的原始訊息（可能含資料表名、堆疊片段）會原封不動私訊給前端——與同檔案對 `MessageConversionException` 特意包裝繁中訊息、避免洩漏 Jackson 細節的用心（`:121-127`）自相矛盾。應加一層「非預期例外回統一訊息」。
3. **每日配額的三個縫**：(a) 檢查與寫入無交易、無鎖，並發發言可繞過上限（與書籤 TOCTOU 同型）；(b) 配額計數排除已軟刪留言（`StationChatMapper.xml:164` `deleted_at IS NULL`），**自刪留言即可回充配額**，刷訊息者可無限循環；(c) 配額窗以 UTC 午夜起算、清理排程卻按伺服器本地時區 03:00 物理清空——兩個「一天」不對齊，且清空後計數歸零等於變相重置配額。
4. **「當日」語意靠排程成功來保證**。匯出 SQL 沒有任何日期過濾（`getAllMessagesByStationId`），之所以等於「當日」純粹因為昨天的資料被清掉了；清理排程失敗的那天，匯出會混入多日資料，Swagger 卻寫死「當日聊天紀錄」。屬於「文件語意依賴另一個元件的成功執行」的隱性耦合，至少該在文件註明。
5. **跨模組直查資料表**：`StationChatDAO` 直接 SELECT `user_trip_plans`、`users`、`membership_tiers`（`getActiveTripPlanOwnerId`、`getMaxDailyChatsByUserId`），繞過 tripPlan／user 模組的 DAO。車站存在性有委派 `MetroService`、旅程擁有權卻自己查表，同一個 Service 內模組邊界的處理方式不一致。

# 6. 旅程規劃

### 1.2 盲點（依優先序）

**① 同名方法兩種行為：這裡的 `enrichTravelTime` 不容錯，列表一筆失配全頁 404。** 聊天室的 `enrichTravelTime` 用 try-catch 包住、算不出只記警告；本模組的同名方法（`TripPlanService:358`）直接讓 `StationNotFoundException` 往外拋，而 `getAllTripPlan` 是 `tripPlans.forEach(this::enrichTravelTime)`——只要任何一筆旅程的起訖站在車站資料重新同步後對不上路線代碼，**整個列表頁 404**，使用者連其他正常旅程都看不到。且每頁預設 8 筆就是 8 次即時 Dijkstra，熱路徑成本可觀。建議統一成容錯版（`travelTimeSeconds` 回 null 即可），並評估結果快取。

**② page/size 未驗證（專案通病第四次出現）**：`size=0` → `Math.ceil` 除以零加上 SQL Server `FETCH NEXT 0 ROWS` 語法錯誤 → 500；負數 page 產生負 OFFSET 同樣炸在 DB 層。四個模組同病，適合做一次全域收斂（共用分頁參數校驗或 `PageRequest` 類 DTO）。

**③ 配額檢查 TOCTOU**：`countActiveByUserId` 與 `insert` 之間無交易無鎖，並發請求可繞過等級上限。本 Service 全類沒有任何 `@Transactional`（對照 `UserService` 五個更新方法都有），與聊天室每日配額是同一款時間縫。

**④ 票價與轉乘次數信任前端傳值**：`farePrice`、`transferCount` 只驗非負，但後端明明具備由起訖站＋票種算出正確票價與轉乘數的能力（捷運模組現成功能），卻不做一致性驗證——會員可以存一筆「淡水到象山 5 元 0 轉乘」的旅程，匯出 Excel 後這些數字被呈現為事實。低風險（只汙染自己的資料），但與「車程時間必定即時計算、不信任儲存值」的嚴謹形成同一個 VO 內的雙重標準。

**⑤ 輕微**：403/404 的差異構成 id 存在性探測（猜 id 拿到 403表示該旅程存在、只是不是你的）；LIKE 關鍵字 `%`/`_` 未跳脫（參數化無注入風險，僅搜尋語意失真）。
