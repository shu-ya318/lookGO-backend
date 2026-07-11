# 0. 全域機制

## 1. 時序圖

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client
    participant JwtFilter as JwtFilter<br/>(OncePerRequestFilter)
    participant Redis as Redis<br/>(jti 黑名單)
    participant UDS as UserDetailsServiceImpl<br/>(SQL Server)
    participant SC as SecurityContext
    participant Authz as FilterSecurityInterceptor<br/>(授權層 / AuthorizationFilter)
    participant MS as MethodSecurity<br/>(@PreAuthorize AOP)
    participant Ctrl as Controller
    participant Init as AdminInitializer<br/>(ApplicationRunner)

    rect rgba(180, 180, 180, 0.15)
        Note over Init: 【啟動時・一次性】容器就緒後執行
        Init->>Init: existsByEmail(app.admin.email)?
        alt 管理員不存在
            Init->>Init: BCrypt 加密密碼 → createUser(ROLE_ADMIN)
        else 已存在
            Init-->>Init: 略過，防重複建立
        end
    end

    Note over Client, Ctrl: 【每次 HTTP 請求】
    Client->>JwtFilter: HTTP 請求 (Authorization: Bearer token)

    alt 公開路徑（SecurityConstants.API_PUBLIC_ALL）
        JwtFilter->>Authz: shouldNotFilter = true，整個跳過 JWT 驗證
        Authz->>Ctrl: permitAll 放行
        Ctrl-->>Client: 200 回應
    else 受保護路徑
        JwtFilter->>JwtFilter: 檢查 Bearer token 存在性與簽章有效性
        JwtFilter->>Redis: isAccessTokenJtiInBlacklist(jti)
        Redis-->>JwtFilter: 黑名單查詢結果

        alt token 有效且不在黑名單
            JwtFilter->>UDS: loadUserByUsername(email)
            UDS-->>JwtFilter: UserDetails（含 ROLE_*；停用帳號拋例外）
            JwtFilter->>SC: setAuthentication(UsernamePasswordAuthenticationToken)
        else 驗證失敗／黑名單命中／例外
            JwtFilter->>SC: clearContext()（不直接回錯，照常放行）
        end

        JwtFilter->>Authz: filterChain.doFilter() 繼續往下

        alt SecurityContext 無身分
            Authz-->>Client: authenticationEntryPoint 統一 401 JSON
        else SecurityContext 有身分（anyRequest().authenticated() 通過）
            Authz->>MS: 進入方法層授權
            alt @PreAuthorize("hasRole('ADMIN')") 端點
                MS->>MS: AOP 驗證 ROLE_ADMIN
                alt 具備 ADMIN 角色
                    MS->>Ctrl: 通過，執行 Controller 方法
                    Ctrl-->>Client: 200 回應
                else 非 ADMIN
                    MS-->>Client: 403 Forbidden
                end
            else 一般會員端點
                MS->>Ctrl: 直接執行 Controller 方法
                Ctrl-->>Client: 200 回應
            end
        end
    end
```

## 2. 重點解釋

- 整個系統採 JWT + Redis 黑名單實現無狀態身分驗證，並依照角色進行授權
  - `SecurityConfig`
    - 關閉 CSRF 與 Session
    - 把 `JwtFilter` 插在 `UsernamePasswordAuthenticationFilter` 之前
    - 每個請求依「公開路徑跳過 → 簽章驗證 → Redis jti 黑名單 → 載入 UserDetails → 寫入 SecurityContext」六步鏈完成身分識別。
  - 授權分三層：
    - 公開白名單集中在 `SecurityConstants.API_PUBLIC_ALL` 供 `permitAll` 與 `shouldNotFilter` 共用（單一資料來源、改一處全域生效）
    - 會員層靠 `anyRequest().authenticated()`
    - 管理層延後到方法層由 16 個 `@PreAuthorize("hasRole('ADMIN')")` 把關
  - 所有驗證失敗集中處理
    - 都不在 Filter 內回錯
    - 而是清掉 SecurityContext 放行
    - 由 `authenticationEntryPoint` 統一輸出 401 JSON
  - 初始化時建立管理員帳號
    - 由 `AdminInitializer`（ApplicationRunner）
    - 啟動時以設定檔注入的帳密自動建立
    - 先查重再以 BCrypt 落庫
    - 避免硬編碼與重複建立


# 1. 帳號認證

## 1. 時序圖

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client
    participant Ctrl as AuthController
    participant Svc as AuthService
    participant DB as AuthDAO / UserDAO<br/>(SQL Server)
    participant Jwt as JwtUtil
    participant Redis as Redis
    participant Cookie as CookieUtil
    participant Logout as LogoutResultHandler<br/>(LogoutSuccessHandler)

    rect rgba(180, 180, 180, 0.15)
        Note over Client, Cookie: 【註冊／登入】成功當下核發雙 token
        Client->>Ctrl: POST /sign-up 或 /log-in（@Valid DTO）
        Ctrl->>Svc: signup() / login()
        alt 註冊（@Transactional）
            Svc->>DB: existsByEmail 防重複（重複 → 400）
            Svc->>Svc: BCrypt 加密密碼
            Svc->>DB: createUser（useGeneratedKeys 取回自增 id）
        else 登入
            Svc->>DB: getByEmail（查無 → 401）
            Svc->>Svc: 檢查 ACTIVE 狀態 → BCrypt matches（錯誤 → 401）
        end
        Svc->>Jwt: generateAccessToken + generateRefreshToken
        Svc->>Redis: saveRefreshTokenJti(userId, jti, TTL)（白名單）
        Svc-->>Ctrl: AuthVO（雙 token）
        Ctrl->>Cookie: addRefreshTokenCookie（HttpOnly + Secure + SameSite=None）
        Ctrl-->>Client: 200 AuthVO（存取 token 在回應本體）
    end

    rect rgba(180, 180, 180, 0.15)
        Note over Client, Redis: 【忘記密碼 → 重設密碼】15 分鐘一次性 token
        Client->>Ctrl: POST /forget-password（email + cellphone）
        Ctrl->>Svc: forgetPassword()
        Svc->>DB: getByEmail + 比對 cellphone
        Note right of Svc: 兩種失敗回同一句錯誤訊息<br/>避免帳號探測（401）
        Svc->>Svc: SecureRandom 生成 32-byte URL-safe token
        Svc->>Redis: saveResetPasswordToken(token → email, 15 分鐘 TTL)
        Ctrl-->>Client: 200 回傳重設 token
        Client->>Ctrl: POST /reset-password（token + 新密碼）
        Ctrl->>Svc: resetPassword()（@Transactional）
        Svc->>Redis: getEmailByResetPasswordToken（null → 401）
        Svc->>DB: updatePasswordByEmail（BCrypt 加密後更新）
        Svc->>Redis: deleteResetPasswordToken（用過即失效）
        Ctrl-->>Client: 200 密碼重設成功
    end

    rect rgba(180, 180, 180, 0.15)
        Note over Client, Redis: 【刷新 token】三關驗證 + rotation
        Client->>Ctrl: POST /refresh-tokens（瀏覽器自動帶 refreshToken Cookie）
        Ctrl->>Cookie: getRefreshTokenFromCookie
        Ctrl->>Jwt: validateRefreshToken（第一關：簽章，無效 → 401）
        Ctrl->>Svc: refreshTokens()
        Svc->>DB: getByEmail + ACTIVE 檢查（第二關：帳號狀態）
        Svc->>Redis: getRefreshTokenJti(userId) 比對 jti（第三關：白名單，不符 → 401）
        Svc->>Jwt: 重新核發雙 token
        Svc->>Redis: 覆寫 refreshTokenJti（rotation，舊刷新 token 即刻失效）
        Ctrl->>Cookie: 換發新 refreshToken Cookie
        Ctrl-->>Client: 200 新 AuthVO
    end

    rect rgba(180, 180, 180, 0.15)
        Note over Client, Logout: 【登出】即時撤銷（Controller 是 Swagger 空殼）
        Client->>Logout: POST /log-out（Spring Security LogoutFilter 攔截）
        Logout->>Jwt: 驗證存取 token，取 jti 與剩餘 TTL
        Logout->>Redis: saveAccessTokenJtiToBlacklist(jti, 剩餘 TTL)
        Logout->>Redis: deleteRefreshTokenJti（撤銷刷新 token 白名單）
        Logout->>Cookie: clearRefreshTokenCookie
        Logout-->>Client: 200 登出成功（JSON）
    end
```

## 2. 重點解釋

註冊或登入成功「當下」即核發雙 token：存取 token 放回應本體、刷新 token 由 `CookieUtil` 寫入 HttpOnly + Secure + SameSite=None 的 Cookie（防 XSS 竊取），同時把刷新 token 的 jti 以 userId 為 key 存入 Redis 白名單。`/refresh-tokens` 採 rotation 設計，連過三關才換發——簽章有效性、DB 帳號 ACTIVE 狀態、Redis 白名單 jti 比對——換發後覆寫白名單使舊刷新 token 立即失效，被竊取的舊 token 無法重放。忘記密碼先驗 email 與手機一致（兩種失敗回同一句錯誤訊息、避免帳號探測），以 SecureRandom 生成 15 分鐘限時的一次性 token 存 Redis，重設成功即刪除。登出時 Controller 只是 Swagger 空殼，實際由 `LogoutResultHandler` 把存取 token 的 jti 依剩餘壽命寫入 Redis 黑名單並清除 Cookie，達成無狀態架構下的「登出即撤銷」。審查發現一處串接缺陷：登出刪白名單時傳的是 email、但儲存時 key 是 userId（`LogoutResultHandler:62` vs `AuthService:233`），導致刷新 token 白名單實際未被刪除、登出後 TTL 內仍可換發新 token，需修正 key 一致性。


# 2. 臺北捷運資訊

## 1. 時序圖

```mermaid
sequenceDiagram
    autonumber
    participant Client as 客戶端
    participant Filter as JwtFilter
    participant MC as MetroController
    participant MS as MetroService
    participant RG as MetroRouteGraphService
    participant FB as MetroForkBranchRouteGraphService
    participant MSC as MetroSyncController
    participant SCH as MetroSyncScheduler
    participant SS as MetroSyncService
    participant TDXC as TDXApiClientConfig
    participant DTC as DataTaipeiApiClientConfig
    participant Redis as Redis
    participant EXT as TDX / DataTaipei API
    participant DAO as MetroDAO
    participant DB as SQL Server

    rect rgb(230, 240, 255)
    Note over Client, DB: 一、公開查詢（訪客）— 以起訖站路徑查詢為例
    Client->>Filter: POST /api/v1/metro/get-origin-destination-detail
    Note right of Filter: 路徑列於 API_PUBLIC_ALL，shouldNotFilter 直接跳過驗證
    Filter->>MC: 放行
    MC->>MS: getOriginDestinationDetail(stationRouteDTO)
    MS->>MS: 驗證票種(1/4/5/7)與路線策略(1/2)，不合法丟 IllegalArgumentException
    MS->>DAO: getAllLine / getAllStation / getAllLineStation / getAllLineTransfer
    DAO->>DB: SELECT 全量路網基礎資料
    DB-->>MS: 路線、車站、關聯、換乘資料
    alt 起訖為同一實體車站（以 stationId 比對，非代碼字串）
        MS->>RG: buildSameStationResult()
        RG-->>MS: 單站結果（票價固定 20 元）
    else 一般情形
        MS->>RG: buildAdjacencyList(策略權重)
        RG->>FB: isSecondaryBranchStation / addBranchEdges
        FB-->>RG: 略過線性推導，改建 Y 字分岔支線邊
        MS->>RG: findRoute(多起點多終點 Dijkstra)
        RG-->>MS: DijkstraResult(path, prevIsTransfer)
        MS->>RG: buildRouteSegments / calculateTotalTime / calculateTransferTime
        MS->>DAO: getFareByStationCodesAndType()
        DAO->>DB: SELECT 兩站票價
    end
    MS-->>MC: OriginDestinationDetailVO
    MC-->>Client: 200 OK（查無車站 → StationNotFoundException → 404）
    end

    rect rgb(255, 245, 230)
    Note over Client, DB: 二、管理員手動同步（6 端點皆 @PreAuthorize ADMIN）
    Client->>Filter: POST /api/v1/metro/sync/sync-all-station (Bearer token)
    Filter->>Filter: 六步驗證鏈 → 寫入 SecurityContext
    Filter->>MSC: 放行 → @PreAuthorize("hasRole('ADMIN')") AOP 驗角色
    MSC->>SS: syncAllStation() [@Transactional]
    SS->>TDXC: sendGetRequest("/Station")
    TDXC->>Redis: getTdxAccessToken()
    alt Redis 快取命中
        Redis-->>TDXC: access token
    else 快取未命中
        TDXC->>EXT: OAuth2 client_credentials 換發 token
        TDXC->>Redis: 存入 token（效期提前 60 秒失效）
    end
    TDXC->>EXT: GET /v2/Rail/Metro/Station/TRTC
    Note right of TDXC: 401 → 清 Redis 快取重發一次；429 → 等待 90 秒重試一次
    EXT-->>SS: TDX 車站名稱資料
    SS->>DTC: sendGetRequest(車站設施資料集 id)
    DTC->>EXT: GET data.taipei 車站設施
    EXT-->>SS: DataTaipei 設施資料
    SS->>SS: 以站名合併雙來源（板橋別名正規化、轉乘站重複站名去重）
    loop 每批 150 筆（每筆 13 參數，避開 SQL Server 單次 2100 參數上限）
        SS->>DAO: upsertAllStation(batch)
        DAO->>DB: MERGE [stations]（比對鍵 original_name_zh_tw，不覆寫管理端改名）
    end
    DB-->>Client: 整批成功才提交，任一失敗整批回滾 → 200 OK
    end

    rect rgb(235, 255, 235)
    Note over SCH, DB: 三、排程自動同步（每週日 23:00 ＋ 首次部署初始化）
    SCH->>SS: ApplicationReadyEvent → isMetroDataEmpty()
    alt 路線資料表為空（首次部署或容器初始化）
        SCH->>SS: 立即執行一次 syncAllDataPipeline()
    else 已有資料（一般重啟）
        SCH->>SCH: 跳過，等待 cron 0 0 23 * * SUN
    end
    SCH->>SS: 依外鍵相依順序執行：Line、Station → LineStation → 累計時間、換乘、票價
    Note right of SS: 整段 try-catch 記 log，單次失敗不影響下次排程
    end
```

## 2. 重點解釋

本模組拆成「查詢」與「同步」兩條互不干擾的路徑：8 個公開查詢端點集中宣告在 `SecurityConstants.API_PUBLIC_ALL`（`SecurityConfig` 的 permitAll 與 `JwtFilter` 的 shouldNotFilter 共用同一份白名單，改一處全域生效），管理與同步端點則全數掛 `@PreAuthorize("hasRole('ADMIN')")` 作為登入後的第二道防線。查詢側的亮點是路徑演算法：`MetroRouteGraphService` 以多起點多終點 Dijkstra 處理「換乘站在不同路線有多個代碼」的等價起訖問題，策略 1（最少轉乘）用 1,000,000 的巨大權重常數讓轉乘次數成為主要比較依據、實際車程秒數作次要依據；無法用 station_sequence 線性推導的 Y 字分岔（中和新蘆線、新北投、小碧潭）則由 `MetroForkBranchRouteGraphService` 以人工維護的支線站序覆蓋建邊。同步側是雙來源合併——TDX 給路線、車站、票價、行駛時間，DataTaipei 給車站設施，以 `original_name_zh_tw` 作為同步比對鍵（而非顯示名稱），確保管理員改站名後不會在下次同步被誤判為新站；6 個同步方法皆 `@Transactional` 加批次 MERGE，整批成功才提交。外部 API 防禦包含：TDX OAuth2 token 快取於 Redis（效期提前 60 秒失效）、401 清快取重發、429 退避 90 秒重試、票價 API 分頁請求並以固定間隔節流，符合 TDX 基礎會員每分鐘 5 次的速率限制。


# 3. 會員

> 對應用例：**UC_A 修改個人資料**、**UC_B 變更密碼**（一般會員）；**UC_C 查詢會員名單**、**UC_D 啟用停用會員帳號**（管理員）。
> 檔案：`UserController.java`、`UserService.java`、`UserDAO.java`、`mappers/UserMapper.xml`

## 1. 時序釐清與盲點審查

### 1.1 動態執行順序與依賴關係

- **共同前置——登入**：四個用例都必須先完成帳號認證，由 `JwtFilter` 六步驗證鏈把身分寫入 SecurityContext。UC_A／UC_B 到此即可通行（會員層 `anyRequest().authenticated()`）；UC_C／UC_D 還要再過方法層 `@PreAuthorize("hasRole('ADMIN')")`（`UserController:91、201`）這第二道防線。
- **UC_A ⟂ UC_B——互相獨立**：修改個資與變更密碼無先後順序，可任意穿插。兩者共用同一套防越權骨架：`getAuthenticatedEmail()`（`UserService:235-237`，身分一律取自 SecurityContextHolder、不收前端 userId）→ `getByEmail` 存在檢查 → DAO 更新，全部掛 `@Transactional`。
- **UC_C → UC_D——資料依賴**：`update-status` 的輸入 `userId` 實務上來自 `get-all-user` 回傳的 `UserVO.id`，管理員必然「先查名單、再停權」。這是操作上的前置條件而非同一次執行內的行為相依，正好呼應 use case 圖「前置條件不畫 include/extend」的記法取捨。
- **UC_D → 終結目標會員的所有後續用例——跨用例副作用**：停權寫入 DB 並刪除 Redis 刷新 token 白名單後，被停權者下一次任何請求（含 UC_A／UC_B）會在 `JwtFilter` → `UserDetailsServiceImpl:44-47` 的每請求 DB 重載被擋（拋例外 → SecurityContext 清空 → 統一 401），`/refresh-tokens` 也因白名單已刪而無法換發。UC_D 是四者中唯一會改變其他用例可達性的操作。
- **UC_A 內部順序——升級副作用不可對調**：`updateBirthDate` 先寫出生日期、再於同一交易內嘗試升級 PREMIUM（`UserService:162-164`），順序固定——升級 SQL 的 `WHERE birth_date IS NOT NULL` 依賴前一步已落庫。


### 1.3 Coding style 一致性判定

**遵守的部分**：建構子注入、SLF4J 日誌、DTO 進／VO 出、`@Valid` + Jakarta Validation 入口驗證、五個寫入方法皆 `@Transactional`（`UserService:107-228`）、Swagger 標註完整、`UpdatePasswordDTO.toString` 遮罩密碼、例外統一交 `GlobalExceptionHandler` 分流（404／401／403）、時間一律以 UTC 存取——與帳號認證、捷運模組的既有款式一致。

**待修改處**，按重要性排序：

| #   | 位置                             | 問題                                                                                                                                                                                                 | 修改建議                                                                                                            |
| --- | -------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| 1   | `UserDAO:35` vs `UserMapper.xml` | **【死碼＋潛在執行期錯誤】**介面宣告 `getAll()` 但 XML 沒有對應的 statement，全案也無人呼叫；一旦被呼叫會拋 MyBatis `BindingException`                                                               | 直接刪除該方法；若日後需要再連同 XML 一起補                                                                         |
| 2   | `UserService:129-144`            | **【安全盲點】變更密碼後未撤銷既有 token**——存取 token 仍有效、刷新 token 白名單未刪，其他裝置的 session 不會被踢下線；系統已有現成的 `deleteRefreshTokenJti`（`updateStatus:223` 就有串），此處漏接 | 密碼更新成功後呼叫 `redisService.deleteRefreshTokenJti(user.getId().toString())`，強制其他裝置重新登入              |
| 3   | `UserController:93-96`           | `page`／`size` 無驗證：`size=0` 時 `totalPages` 除以零（`Math.ceil` → Infinity 再窄化轉型）且 SQL Server `FETCH NEXT 0 ROWS` 為語法錯誤 → 500；負數同理                                              | 參數加 `@Min(0)`（page）、`@Min(1)`（size），讓錯誤走 400 分流                                                      |
| 4   | `UserFilterRequestDTO` 全檔      | 全案無任何引用（`getAllUser` 以 `@RequestParam` 收 keyword，未用此 DTO），死碼                                                                                                                       | 刪除檔案                                                                                                            |
| 5   | `UserMapper.xml:61-67`           | 升級守門條件 `membership_tier_id != #{membershipTierId}`：現只有 BASIC／PREMIUM 兩級無礙，但未來若新增更高等級，填生日會被此 SQL「降級」回 PREMIUM                                                   | 條件改為 `membership_tier_id < #{membershipTierId}`（或明確 `= BASIC 的 id`），語意從「不同就改」收斂為「只升不降」 |
| 6   | `UserDAO` 全檔                   | 8 格縮排，與專案 4 格不一致（同 `AuthDAO`、`MetroDAO` 已列問題，DAO 層通病）                                                                                                                         | 統一 4 格，以 formatter 設定固定                                                                                    |
| 7   | `UserService:35`                 | 類別 Javadoc 寫「處理使用者資料**查詢**相關的業務邏輯」，實際包含五個更新方法，註解與職責不符                                                                                                        | 修正為「處理使用者資料查詢與更新相關的業務邏輯」                                                                    |
| 8   | `UserMapper.xml:19、31`          | 模糊搜尋的 keyword 未跳脫 `%` 與 `_` 萬用字元，使用者輸入會改變搜尋語意（`#{}` 已參數化、無注入風險，僅語意問題）                                                                                    | LIKE 加 `ESCAPE` 處理，或於文件註明此為預期行為                                                                     |
| 9   | `UserController:198`             | `update-status` 的 403 Swagger 只寫「權限不足」，漏了「不得變更管理員帳號狀態」（`AdminStatusModificationException` → 403，`GlobalExceptionHandler:187`）也是同一狀態碼                              | 403 說明補充兩種情境                                                                                                |

## 2. 各 Use Case 時序圖

### UC_A 修改個人資料（update-username／update-birth-date／update-cellphone）

```mermaid
sequenceDiagram
    autonumber
    actor Member as 一般會員
    participant Ctrl as UserController
    participant Svc as UserService
    participant DAO as UserDAO<br/>(SQL Server)

    Note over Member, DAO: 前置：JwtFilter 六步驗證鏈已通過，身分在 SecurityContext（見「全域機制」）
    Member->>Ctrl: POST /update-username｜/update-birth-date｜/update-cellphone（@Valid DTO）
    Note right of Ctrl: 入口擋格式（錯誤 → 400）：<br/>@NotBlank／@PastOrPresent 生日不得未來日／@Pattern 電話 ^0\d{8,9}$
    Ctrl->>Svc: updateXxx(dto) [@Transactional]
    Svc->>Svc: getAuthenticatedEmail()（取自 SecurityContextHolder，不收前端 userId——防越權根本設計）
    Svc->>DAO: getByEmail(email)
    alt 查無使用者
        Svc-->>Member: UserNotFoundException → 404
    else 使用者存在
        alt 更新名稱／電話
            Svc->>DAO: updateUsernameByEmail / updateCellphoneByEmail（updated_at 帶 UTC 時間）
            Svc-->>Ctrl: MessageVO「更新成功!」
        else 更新出生日期（含會員升級副作用）
            Svc->>DAO: updateBirthDateByEmail(email, birthDate, now)
            Svc->>DAO: updateMembershipTierByEmail(email, PREMIUM, now)
            Note right of DAO: WHERE birth_date IS NOT NULL<br/>AND membership_tier_id != PREMIUM<br/>「檢查＋更新」在 DB 層原子化，防併發重複升級
            alt 影響筆數 > 0（原為 BASIC）
                Svc-->>Ctrl: MessageVO「出生日期更新成功，會員等級已自動升級為 PREMIUM!」
            else 已是 PREMIUM
                Svc-->>Ctrl: MessageVO「出生日期更新成功!」
            end
        end
        Ctrl-->>Member: 200 OK
    end
```

### UC_B 變更密碼（update-password）

```mermaid
sequenceDiagram
    autonumber
    actor Member as 一般會員
    participant Ctrl as UserController
    participant Svc as UserService
    participant Enc as BCryptPasswordEncoder
    participant DAO as UserDAO<br/>(SQL Server)

    Note over Member, DAO: 前置：已登入，身分在 SecurityContext
    Member->>Ctrl: POST /update-password（oldPassword + newPassword，@Valid）
    Note right of Ctrl: @NotBlank + 新密碼 @Size(8-20)（錯誤 → 400）
    Ctrl->>Svc: updatePassword(dto) [@Transactional]
    Svc->>Svc: getAuthenticatedEmail()
    Svc->>DAO: getByEmail(email)（查無 → UserNotFoundException 404）
    Svc->>Enc: matches(oldPassword, user.password)
    alt 舊密碼錯誤
        Svc-->>Member: InvalidCredentialsException → 401
    else 舊密碼正確
        Svc->>Enc: encode(newPassword)（BCrypt 單向雜湊）
        Svc->>DAO: updatePasswordByEmail(email, hash, updatedAt)
        Ctrl-->>Member: 200「密碼更新成功!」
        Note over Svc, DAO: ⚠ 既有存取／刷新 token 皆未撤銷，<br/>其他裝置 session 仍有效（見 1.3 表 #2）
    end
```

### UC_C 查詢會員名單（get-all-user）

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 管理員
    participant MS as MethodSecurity<br/>(@PreAuthorize AOP)
    participant Ctrl as UserController
    participant Svc as UserService
    participant DAO as UserDAO<br/>(SQL Server)

    Note over Admin, DAO: 前置：已登入且 JwtFilter 寫入 SecurityContext（ROLE 由 DB 每請求重載）
    Admin->>MS: POST /get-all-user?keyword=&page=0&size=8
    alt 非 ADMIN
        MS-->>Admin: 403 Forbidden
    else 具備 ROLE_ADMIN
        MS->>Ctrl: getAllUser(keyword, page, size)
        Ctrl->>Svc: getAllUser(keyword, page, size)
        Svc->>DAO: getAllPaginated(keyword, page*size, size)
        Note right of DAO: username／email LIKE '%keyword%'<br/>ORDER BY id + OFFSET/FETCH（SQL Server 分頁）
        Svc->>DAO: countAll(keyword)（與列表共用同一組 <where> 過濾條件）
        Svc->>Svc: users.stream().map(toVO)（轉 UserVO，不含密碼；時間補 UTC 時區）
        Svc-->>Ctrl: PaginatedVO<UserVO>（content／page／size／totalElements／totalPages）
        Ctrl-->>Admin: 200 OK
    end
```

### UC_D 啟用停用會員帳號（update-status）

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 管理員
    participant MS as MethodSecurity<br/>(@PreAuthorize AOP)
    participant Ctrl as UserController
    participant Svc as UserService
    participant DAO as UserDAO<br/>(SQL Server)
    participant Redis as Redis
    actor Target as 被停權會員

    Admin->>MS: POST /update-status（userId + status）
    Note right of MS: 非 ADMIN → 403；status 非 ACTIVE/DISABLED<br/>→ enum 反序列化失敗 → 400
    MS->>Ctrl: updateStatus(dto)（@Valid：兩欄位皆 @NotNull）
    Ctrl->>Svc: updateStatus(dto) [@Transactional]
    Svc->>DAO: getById(userId)
    alt 查無使用者
        Svc-->>Admin: UserNotFoundException → 404
    else 目標為 ADMIN
        Svc-->>Admin: AdminStatusModificationException → 403（不得變更管理員帳號狀態）
    else 狀態已相同
        Svc-->>Admin: 200「已經是指定的狀態，不需更新!」（冪等短路，不落 DB）
    else 需變更
        Svc->>DAO: updateStatusById(id, status, updatedAt)
        opt 目標狀態 = DISABLED
            Svc->>Redis: deleteRefreshTokenJti(userId)（撤銷刷新 token 白名單，key 與儲存端一致）
        end
        Svc-->>Admin: 200「更新使用者狀態成功!」
    end

    Note over Target, Redis: 【停權生效時序】存取 token 未進黑名單，<br/>但下一請求 JwtFilter → UserDetailsServiceImpl:44 讀 DB 見 DISABLED 即拋例外 → 401；<br/>刷新 token 白名單已刪 → /refresh-tokens 換發也被擋——雙路徑封死
```

## 3. 重點摘要（給技術主管）

### UC_A 修改個人資料

修改個資拆成名稱、出生日期、電話三支獨立端點，全部走同一套防越權骨架——身分一律從 SecurityContextHolder 取 email、完全不收前端傳的 userId，從設計上杜絕水平越權。Jakarta Validation 在 Controller 入口先擋格式（電話 regex、生日不得為未來日），Service 再做存在檢查，查無回 404，三支方法皆 `@Transactional`。亮點是出生日期的升級副作用：填了生日且原等級為 BASIC，就在同一交易內自動升級 PREMIUM，且升級條件下沉到 SQL 的 WHERE 子句，「檢查＋更新」在資料庫層原子化、防併發重複升級。回應依 DAO 影響筆數區分「有升級／無升級」兩種文案，前端不需再查一次。

### UC_B 變更密碼

變更密碼採「先驗舊密碼、再 BCrypt 加密落庫」的兩段式，舊密碼比對失敗丟 `InvalidCredentialsException` 回 401，與參數格式錯誤的 400 明確分流。身分同樣取自 SecurityContext，整個方法掛 `@Transactional`，新密碼長度 8–20 由入口 `@Size` 把關。已知盲點：改密碼成功後既有的存取與刷新 token 都未撤銷，其他裝置的 session 不會被踢下線——系統已有現成的 `deleteRefreshTokenJti` 機制（停權流程就有串接），這裡補一行呼叫即可，是低成本高價值的補強點。

### UC_C 查詢會員名單

查詢會員名單是純讀取操作，安全完全靠方法層 `@PreAuthorize("hasRole('ADMIN')")` AOP 把關，且角色每請求從 DB 重載、不信 token claim。支援 username／email 模糊搜尋與分頁，SQL Server 用 OFFSET/FETCH 實作，列表查詢與總數計算共用同一組動態 `<where>` 條件，保證分頁資訊一致。回傳統一封裝為 `PaginatedVO<UserVO>`，Entity 轉 VO 時不帶密碼欄位、時間補上 UTC 時區。待補缺口是 page／size 未驗證，size=0 會觸發除零與 SQL 語法錯誤而回 500。

### UC_D 啟用停用會員帳號

停權流程有四道業務防線依序把關：目標存在（404）、目標非管理員（403，防止管理員互相鎖帳號）、狀態未重複（冪等短路、不落 DB）、enum 反序列化擋非法狀態值（400）。停權時在同一交易內刪除該使用者的 Redis 刷新 token 白名單，且 key 用 userId、與儲存端一致（登出流程誤傳 email 的 bug 在這裡沒有重演）。存取 token 雖然不進黑名單——系統也無從得知對方手上的 jti——但 `JwtFilter` 每請求都經 `UserDetailsServiceImpl` 重載 DB 狀態，停權者下一次請求即被擋回 401。因此「停權即時生效」靠的是每請求 DB 重載而非黑名單，換發與續用兩條路徑都已封死，講稿的開放問題可以正面回答。

# 4. 車站書籤

> 對應用例：**收藏車站書籤**、**查詢車站書籤**（單筆／列表）、**移除車站書籤**、**匯出書籤 Excel**（皆為一般會員可操作）。
> 檔案：`StationBookmarkController.java`、`StationBookmarkService.java`、`StationBookmarkDAO.java`、`mappers/StationBookmarkMapper.xml`

## 1. 時序釐清與盲點審查

### 1.1 動態執行順序與依賴關係

- **共同前置——登入即可**：四個端點全屬會員層（`anyRequest().authenticated()`），本模組沒有任何 `@PreAuthorize`——先前兩端點（查全部、匯出）Swagger 誤標 403 的問題，是以「修正文件」而非「加 ADMIN 限制」收場，等於確認開放給一般會員是刻意決策。
- **收藏 → 其他三者——資料前置**：查詢、移除、匯出都以「已存在書籤」為前提，移除所需的 `bookmarkId` 實務上來自查詢列表的回傳。這是操作上的前置條件而非行為相依，同樣不畫 include/extend。
- **收藏內部——四步 fail-fast 檢查鏈，順序固定**：當前使用者存在（404）→ 車站存在（404，跨模組問 `MetroService`）→ 未重複收藏（400，只看有效書籤）→ 未達會員等級上限（400，上限 JOIN `membership_tiers` 查表、非硬編碼，與旅程規劃同一套款式）。全部通過才 insert，再以自增 id 回查 JOIN 後的顯示資料組回應。
- **移除 ↔ 收藏——合法循環**：軟刪除只更新 `deleted_at`，所有查詢與匯出立即看不到；重複檢查只認有效書籤，因此「刪除 → 對同站再收藏」是合法路徑，舊列軟刪除、新列另起。
- **匯出——當下快照**：範圍固定為全部有效書籤，不受列表分頁與關鍵字影響；本模組資料無生命週期排程（對比聊天室每日清空），匯出與其他用例無時序耦合。


### 1.3 Coding style 一致性判定

**遵守的部分**：建構子注入、SLF4J、DTO 進／VO 出、`@Valid` 驗證、Swagger 標註完整、例外分流（404／400 各有專屬例外）、時間一律 UTC；Mapper 的 `<sql id="bookmarkVOSelectAndJoins">` 共用片段是全案 Mapper 少見的重用款式，且所有查詢 SQL 皆帶 `deleted_at IS NULL` 過濾（Mapper XML 必查點通過；唯一例外 `getById` 是刻意回原始資料、由 Service 層 filter 判斷已刪）。

**待修改處**，按重要性排序：

| #   | 位置                                         | 問題                                                                                                            | 修改建議                                                                                                                                                                          |
| --- | -------------------------------------------- | --------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `StationBookmarkService:156-166`             | **【安全漏洞・水平越權】**`deleteBookmark` 未驗證擁有權，任何會員可軟刪除他人書籤（詳見 1.2 #1）                | 刪除前比對 `bookmark.getUserId()` 與當前使用者 id，不符丟 403 專屬例外（參考旅程規劃的 `getCurrentUserIdAndCheckOwnership()` 收斂款式），並補 `GlobalExceptionHandler` 對應       |
| 2   | `StationBookmarkController:114-124、158-171` | **【個資外洩疑慮】**列表與匯出回傳全站會員 username／email，端點僅需登入（詳見 1.2 #2）                         | 二擇一：加 `@PreAuthorize("hasRole('ADMIN')")` 限管理員，或改為僅回傳當前使用者自己的書籤                                                                                         |
| 3   | `StationBookmarkService` 全類                | 無任何 `@Transactional`，寫入款式與 user／tripPlan 模組不一致；`createBookmark` 檢查與寫入間有 TOCTOU 競態      | 寫入方法補 `@Transactional`；重複防線下沉 DB——SQL Server filtered unique index：`CREATE UNIQUE INDEX ... ON user_station_bookmarks(user_id, station_id) WHERE deleted_at IS NULL` |
| 4   | `StationBookmarkController:117-118`          | `page`／`size` 未驗證，`size=0` → 除零＋`FETCH NEXT 0 ROWS` SQL 語法錯誤 → 500（與 user 模組同病）              | `@Min(0)`（page）、`@Min(1)`（size），走 400 分流                                                                                                                                 |
| 5   | `StationBookmarkController` 全檔             | 8 格縮排，但同模組的 Service／DAO 是 4 格——同一模組內部縮排都不一致                                             | 統一 4 格，以 formatter 固定                                                                                                                                                      |
| 6   | `StationBookmarkService:229-231`             | 匯出失敗把 `IOException` 換成自訂例外時未帶 cause，遺失根因（同 `RailClientConfig` 列過的問題款式）             | 自訂例外補 `(String, Throwable)` 建構子並傳入 `error`                                                                                                                             |
| 7   | `StationBookmarkService:50-51、81、86`       | 跨模組依賴款式不一：對 metro 走 Service（`existsStationById`）、對 user 卻直接注入**他模組的 DAO**（`UserDAO`） | 統一走對方模組的 Service 介面，DAO 保留為模組內部實作細節                                                                                                                         |
| 8   | `StationBookmarkMapper.xml:28-30、46-48、80` | LIKE 關鍵字未跳脫 `%`／`_` 萬用字元（與 user 模組同病，`#{}` 已參數化、無注入風險）                             | `ESCAPE` 處理或文件註明                                                                                                                                                           |

## 2. 各 Use Case 時序圖

### 收藏車站書籤（create-bookmark）

```mermaid
sequenceDiagram
    autonumber
    actor Member as 一般會員
    participant Ctrl as StationBookmarkController
    participant Svc as StationBookmarkService
    participant Metro as MetroService<br/>(跨模組)
    participant UDAO as UserDAO
    participant BDAO as StationBookmarkDAO<br/>(SQL Server)

    Note over Member, BDAO: 前置：已登入（JwtFilter 驗證通過，會員層即可）
    Member->>Ctrl: POST /create-bookmark（stationId，@Valid @NotNull）
    Ctrl->>Svc: createBookmark(dto)
    Svc->>UDAO: getByEmail(SecurityContext 的 email)（查無 → UserNotFoundException 404）
    Svc->>Metro: existsStationById(stationId)
    alt 車站不存在
        Svc-->>Member: StationNotFoundException → 404
    else 車站存在
        Svc->>BDAO: getActiveBookmarkIdByUserIdAndStationId(userId, stationId)
        alt 已有有效書籤
            Svc-->>Member: BookmarkDuplicateException → 400
        else 未重複
            Svc->>BDAO: getMaxBookmarksByUserId(userId)
            Note right of BDAO: JOIN membership_tiers 依會員等級查上限<br/>（查表、非硬編碼，與旅程規劃同款）
            Svc->>BDAO: countActiveByUserId(userId)
            alt 有效書籤數 ≥ 上限
                Svc-->>Member: BookmarkLimitExceededException → 400
            else 未達上限
                Svc->>BDAO: insert(bookmark)（useGeneratedKeys 取回自增 id，created_at 帶 UTC）
                Svc->>BDAO: getVOById(新 id)（JOIN 車站＋使用者，組顯示資料）
                Svc-->>Ctrl: StationBookmarkVO
                Ctrl-->>Member: 200 OK
            end
        end
    end
```

### 查詢車站書籤（get-bookmark-by-station-name／get-all-bookmark-paginated）

```mermaid
sequenceDiagram
    autonumber
    actor Member as 一般會員
    participant Ctrl as StationBookmarkController
    participant Svc as StationBookmarkService
    participant UDAO as UserDAO
    participant BDAO as StationBookmarkDAO<br/>(SQL Server)

    alt 單筆查詢（依站名，只查自己的）
        Member->>Ctrl: POST /get-bookmark-by-station-name?stationName=淡水
        Ctrl->>Svc: getBookmarkByStationName(stationName)
        Svc->>UDAO: getByEmail(...)（查無 → 404）
        Svc->>BDAO: getActiveVOByUserIdAndStationNameLike(userId, stationName)
        Note right of BDAO: WHERE user_id = 自己 AND deleted_at IS NULL<br/>站名 LIKE 模糊比對，多筆取收藏時間最新一筆（FETCH NEXT 1）
        alt 查無符合
            Svc-->>Member: BookmarkNotFoundException → 404
        else 命中
            Svc-->>Member: 200 StationBookmarkVO
        end
    else 分頁列表（全站資料，未以 userId 過濾）
        Member->>Ctrl: POST /get-all-bookmark-paginated?keyword=&page=0&size=8
        Ctrl->>Svc: getAllBookmark(keyword, page, size)
        Svc->>BDAO: getAllPaginated(keyword, offset, size) + countAll(keyword)
        Note right of BDAO: 皆帶 deleted_at IS NULL；關鍵字比對車站名／使用者名／email 三欄<br/>ORDER BY created_at DESC + OFFSET/FETCH
        Svc-->>Member: 200 PaginatedVO<StationBookmarkVO>（⚠ 含全站會員 username/email，見 1.2 #2）
    end
```

### 移除車站書籤（delete-bookmark）

```mermaid
sequenceDiagram
    autonumber
    actor Member as 一般會員
    participant Ctrl as StationBookmarkController
    participant Svc as StationBookmarkService
    participant BDAO as StationBookmarkDAO<br/>(SQL Server)

    Member->>Ctrl: POST /delete-bookmark（bookmarkId，@Valid @NotNull）
    Ctrl->>Svc: deleteBookmark(bookmarkId)
    Svc->>BDAO: getById(bookmarkId)（刻意不濾軟刪除，取原始資料）
    alt 查無、或已被軟刪除（Service 端 filter deletedAt == null）
        Svc-->>Member: BookmarkNotFoundException → 404
    else 書籤有效
        Note over Svc: ⚠ 缺擁有權檢查：未比對 bookmark.userId 與當前使用者<br/>（Swagger 標了 403，但程式沒有任何 403 的產生來源——見 1.2 #1）
        Svc->>BDAO: softDeleteById(id, deletedAt = UTC now)（軟刪除，非物理刪除）
        Svc-->>Member: 200「書籤刪除成功!」
    end
```

### 匯出書籤 Excel（get-excel）

```mermaid
sequenceDiagram
    autonumber
    actor Member as 一般會員
    participant Ctrl as StationBookmarkController
    participant Svc as StationBookmarkService
    participant BDAO as StationBookmarkDAO<br/>(SQL Server)
    participant POI as Apache POI<br/>(XSSFWorkbook)

    Member->>Ctrl: POST /get-excel
    Ctrl->>Svc: exportBookmarkExcel()
    Svc->>BDAO: getAllActive()（全部有效書籤，不受列表分頁與關鍵字影響）
    Svc->>POI: 建表頭（書籤id／使用者名稱／Email／車站名稱／收藏時間UTC）
    loop 每筆書籤
        Svc->>POI: createRow 寫入一列
    end
    Svc->>POI: 末列寫入「【總計】共 N 筆書籤」
    POI-->>Svc: byte[]（IOException → StationBookmarkExportExcelFailedException → 500）
    Svc-->>Ctrl: byte[]
    Ctrl-->>Member: 200 attachment（filename*=utf-8'' RFC 5987 編碼中文檔名）
```

## 3. 重點摘要（給技術主管）

### 收藏車站書籤

新增書籤走「使用者存在 → 車站存在 → 未重複收藏 → 未達等級上限」四步 fail-fast 檢查鏈，順序固定、前兩步 404 後兩步 400。數量上限不是硬編碼常數，而是 JOIN `membership_tiers` 依會員等級查表，與旅程規劃共用同一套查表款式，調整上限只動資料不動程式。車站存在性跨模組委派給 `MetroService` 判斷，維持模組邊界。寫入後以自增 id 回查 JOIN 後的完整顯示資料組回應，前端一次拿到車站與使用者資訊。已知縫隙：全程無交易且檢查與寫入分離，併發下可繞過重複與上限檢查，建議補 filtered unique index 由 DB 收底。

### 查詢車站書籤

查詢分兩支：單筆查詢依站名模糊比對且 SQL 綁定 `user_id = 自己`，多筆命中時取收藏時間最新一筆，查無回 404；分頁列表支援站名／使用者名／email 三欄關鍵字搜尋，列表與總數共用同一組過濾條件。所有查詢 SQL 一律帶 `deleted_at IS NULL`，軟刪除資料徹底隱形，這是本模組 Mapper 的必查點且全數通過。要注意的反差是：單筆有綁使用者、列表卻回傳全站所有會員的書籤與 email，一般會員即可翻頁撈個資，資料範圍的決策需要重審。

### 移除車站書籤

移除採軟刪除設計——只把 `deleted_at` 蓋上 UTC 時間戳，不做物理刪除，資料可回溯且立即從所有查詢與匯出消失；同站「刪了再收」是合法循環，因為重複檢查只認有效書籤。存在性檢查刻意用不濾軟刪除的 `getById` 取原始資料、在 Service 層判斷已刪與否，已刪與不存在同樣回 404、不洩漏資料痕跡。**本模組最重要的審查發現在這裡：整條流程沒有比對書籤擁有者與當前登入者，任何會員可列舉連號 id 刪光他人書籤**——Swagger 標了 403 但程式沒有對應檢查，屬於漏實作而非設計，必須比照旅程規劃的擁有權檢查單一入口補上。

### 匯出書籤 Excel

匯出用 Apache POI 產出 xlsx，範圍固定是「全部有效書籤」的當下快照，不受列表分頁與關鍵字影響，末列附總計筆數。回應以 RFC 5987 的 `filename*=utf-8''` 編碼處理中文檔名，避免跨瀏覽器亂碼。失敗時包成專屬例外走統一 500 分流，但目前未帶原始 `IOException` 作 cause，除錯時會遺失根因。與聊天室匯出不同，本模組資料沒有每日清理排程，匯出純屬便利功能、無時序耦合；但它同樣輸出全站會員 email，個資範圍問題與列表查詢一體適用。

# 5. 車站聊天室

> 對應檔案：`WebSocketConfig.java`、`StompAuthChannelInterceptor.java`、`StationChatController.java`、`StationChatStompController.java`、`StationChatService.java`、`StationChatDAO.java`、`StationChatMapper.xml`、`StationChatMessageCleanupScheduler.java`

## 1. 時序釐清與盲點審查

### 1.1 動態執行順序與依賴關係

**雙協定、雙驗證路徑**。本模組是全系統唯一同時走 HTTP 與 STOMP（WebSocket）兩種協定的模組，兩條路徑的驗證時機完全不同：

- **HTTP 路徑**（查詢留言／公告、公告管理、匯出 Excel）：照常走 `JwtFilter` 每請求驗證，管理端點再疊 `@PreAuthorize("hasRole('ADMIN')")`（`StationChatController:124、152、174、196`，即全域機制講的「聊天室管理 4」）。
- **STOMP 路徑**（發言、刪言）：HTTP 的 `JwtFilter` 攔不到 WebSocket frame，因此 handshake 端點 `/ws/**` 直接列入 `SecurityConstants.API_PUBLIC_ALL`（`SecurityConstants:30`）放行，驗證延後到 STOMP **CONNECT 這一個指令**由 `StompAuthChannelInterceptor` 執行（`StompAuthChannelInterceptor:66-109`）——缺 token、簽章無效、jti 在 Redis 黑名單三種情況都丟 `StompAuthException` 拒絕連線，通過後把 Principal 綁進整條 STOMP session，**後續 SEND／SUBSCRIBE 一律放行不重驗**。

**WS 生命週期的固定順序**：CONNECT（驗證一次）→ SUBSCRIBE `/topic/station-chat/{stationId}` → SEND 發言或刪言 → Service 寫 DB 成功後由 `SimpMessagingTemplate` 廣播 `StationChatEventVO`（NEW 帶完整留言、DELETE 只帶 id）給訂閱該站的所有人。廣播一律在 DB 寫入成功之後，順序正確——失敗的操作不會產生幽靈事件。

**發言的檢查鏈**（`StationChatService:228-288`）：使用者存在（404）→ 車站存在（跨模組委派 `MetroService`，404）→ chatType 互斥驗證（TEXT 必有 content 不可帶 tripPlanId；TRIP_PLAN 相反，400）→ 旅程分享再驗「旅程存在且未軟刪」與「擁有者是本人」（`:256-262`，404／403，防止分享他人旅程）→ 每日則數配額（JOIN `membership_tiers` 查表、非硬編碼，超限 400）→ insert 後以自增 id 回查 JOIN 好的 VO 供廣播。

**刪言的雙軌授權**（`StationChatService:300-319`）：取原始留言 → 過濾已軟刪 → **驗證留言歸屬於 URL 上的車站**（`:308`，防止跨站亂刪與錯誤廣播）→ `isOwner || isAdmin` 雙軌判定（`:311-315`）。這是與車站書籤 `deleteBookmark` 最鮮明的對比——同樣是刪除資源，聊天室做對了擁有權檢查，書籤漏了。

**資料生命週期的時序耦合**：留言只活一天——`StationChatMessageCleanupScheduler` 每日 03:00 **物理清空**全站留言（cron `0 0 3 * * *`，整段 try-catch 記 log，單次失敗不擋下次）；管理員匯出 Excel 是「指定車站、不分頁的完整紀錄」，實質是資料歸檔的最後機會，兩功能存在硬性的先後依賴：**匯出必須發生在下一次 03:00 之前，否則資料永久消失**。公告則走軟刪除、長期存活，與留言的生命週期完全不同。


### 1.3 Coding style 一致性判定

**亮點**：`StompAuthChannelInterceptor` 的驗證步驟嚴格比照 `JwtFilter`（含 Redis 黑名單檢查），並附上「必須用 `getAccessor` 取原始 accessor、`wrap` 會遺失 `setUser`」的精準技術註解；`WebSocketConfig:87-99` 讓 STOMP 共用 Spring 管理的 ObjectMapper，避免 HTTP 與 STOMP 的 JSON 行為分裂；Mapper 以 `CASE WHEN` 對已刪旅程分享優雅降級為「該旅程分享已被移除」（`StationChatMapper.xml:17-20`）；`enrichTravelTime` 計算失敗只記警告、不拖垮留言回傳。

待修項目：

| #   | 位置                                                                  | 問題                                                                                                                           | 修改建議                                                      |
| --- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------- |
| 1   | 兩個 Controller、DAO（8 格）vs Service（4 格）                        | 同模組縮排不一致，與書籤模組同病                                                                                               | 統一 4 格並以 formatter 收斂                                  |
| 2   | `StationChatService` 全類                                             | 無任何 `@Transactional`；`sendMessage` 是「查配額→insert→回查」複合操作                                                        | 寫入方法補 `@Transactional`，與 user／tripPlan 模組對齊       |
| 3   | `StationChatStompController:135-141`                                  | 通用例外處理直接回 `exception.getMessage()`，洩漏內部訊息                                                                      | 非預期例外回統一繁中訊息，細節只進 log                        |
| 4   | `StationChatController:78-79、103-104`                                | `page`／`size` 未驗證，`size=0` → 除以零＋SQL Server `FETCH NEXT 0` 語法錯誤 → 500                                             | 加 `@Min(0)`／`@Min(1)`，三個模組一併修                       |
| 5   | `ChatDailyLimitExceededException`、`ChatMessageAccessDeniedException` | 未註冊 `GlobalExceptionHandler`（現只走 STOMP 路徑無妨，但 Service 若被 HTTP 重用即變 500）                                    | 預先補上 429／403 的 handler                                  |
| 6   | `StationChatService:322`、`StationChatDAO:168` Javadoc                | 「磬刪」為錯字                                                                                                                 | 改為「清刪（物理刪除）」                                      |
| 7   | `StationChatService:82` Javadoc                                       | `getMessages` 缺 `@throws StationNotFoundException`（`getAnnouncements` 有）                                                   | 補齊，公開方法 Javadoc 規範一致                               |
| 8   | `StationChatController:118-120`                                       | 匯出 200 的 content 標成 `application/json` String，實為二進位 xlsx；summary 寫「完整聊天紀錄」、response 寫「當日」，語意混用 | mediaType 改 octet-stream，統一「當日」用語並註明依賴清理排程 |
| 9   | `StationChatService:391-425`                                          | 匯出 catch `IOException` 未帶 cause（與書籤同病）                                                                              | 例外建構子傳入原始 error                                      |

## 2. 各 Use Case 時序圖

### 查詢車站訊息與公告（HTTP）

```mermaid
sequenceDiagram
    autonumber
    actor Member as 會員
    participant JF as JwtFilter
    participant Ctrl as StationChatController
    participant Svc as StationChatService
    participant Metro as MetroService
    participant CDAO as StationChatDAO

    Member->>JF: POST /get-message-by-station-id?stationId&page&size
    JF->>JF: 六步驗證（黑名單檢查含）
    JF->>Ctrl: 驗證通過
    Ctrl->>Svc: getMessages(stationId, page, size)
    Svc->>Metro: existsStationById(stationId)
    alt 車站不存在
        Metro-->>Svc: false
        Svc-->>Member: 404 StationNotFoundException
    else 車站存在
        Svc->>CDAO: getMessagesByStationIdPaginated(offset, limit)
        Note right of CDAO: JOIN users + user_trip_plans + stations<br/>已刪旅程分享降級為「該旅程分享已被移除」<br/>deleted_at IS NULL 過濾
        loop 每筆旅程分享留言
            Svc->>Metro: getTravelTimeSecondsByStationIds()（即時算車程，失敗僅記警告）
        end
        Svc->>CDAO: countMessagesByStationId()
        Svc-->>Ctrl: PaginatedVO（新到舊排序）
        Ctrl-->>Member: 200
    end
    Note over Member,CDAO: 公告查詢 get-announcement-by-station-id 同構<br/>差異：查 station_chat_announcements、無車程計算
```

### 即時聊天：連線、發言與刪言（STOMP）

```mermaid
sequenceDiagram
    autonumber
    actor Member as 會員
    participant WS as WebSocketConfig(/ws)
    participant Auth as StompAuthChannelInterceptor
    participant Stomp as StationChatStompController
    participant Svc as StationChatService
    participant CDAO as StationChatDAO
    participant Broker as SimpleBroker(/topic)

    rect rgb(235, 245, 255)
        Note over Member,Auth: 階段一：CONNECT（唯一驗證點）
        Member->>WS: WebSocket handshake（/ws 在公開白名單）
        Member->>Auth: STOMP CONNECT + Bearer token
        Auth->>Auth: token 存在 → 簽章有效 → jti 不在黑名單
        alt 任一步失敗
            Auth-->>Member: StompAuthException 拒絕連線
        else 通過
            Auth->>Auth: 載入 UserDetails，setUser() 綁定 Principal 至整條 session
            Note right of Auth: 之後 SEND 不再重驗——停權/登出/過期踢不掉已連線者
        end
        Member->>Broker: SUBSCRIBE /topic/station-chat/{stationId}
    end

    rect rgb(235, 255, 240)
        Note over Member,Broker: 階段二：發言
        Member->>Stomp: SEND /app/station-chat/{stationId}/send-message
        Stomp->>Svc: sendMessage(stationId, dto, principal.getName())
        Svc->>CDAO: 使用者存在(404) → 車站存在(404) → chatType 互斥驗證(400)
        opt chatType = TRIP_PLAN
            Svc->>CDAO: getActiveTripPlanOwnerId()（查無→404；非本人→403 不得分享他人旅程）
        end
        Svc->>CDAO: getMaxDailyChatsByUserId()（JOIN membership_tiers 查表）
        Svc->>CDAO: countTodayMessagesByUserId(UTC 今日零時起)
        alt 已達每日上限
            Svc-->>Member: ChatDailyLimitExceededException → /user/queue/errors 私訊
        else 未達上限
            Svc->>CDAO: insertMessage() 後以自增 id 回查完整 VO
            Svc-->>Stomp: StationChatMessageVO
            Stomp->>Broker: convertAndSend(NEW 事件)
            Broker-->>Member: 廣播給訂閱該站的所有人
        end
    end

    rect rgb(255, 245, 235)
        Note over Member,Broker: 階段三：刪言（本人或 ADMIN）
        Member->>Stomp: SEND /app/station-chat/{stationId}/delete-message/{messageId}
        Stomp->>Svc: deleteMessage(stationId, messageId, email)
        Svc->>CDAO: getMessageById() → 過濾已軟刪 → 驗證留言屬於該車站(404)
        alt 非本人且非 ADMIN
            Svc-->>Member: ChatMessageAccessDeniedException → 私訊發送者
        else isOwner or isAdmin
            Svc->>CDAO: softDeleteMessageById(UTC 時間戳)
            Stomp->>Broker: convertAndSend(DELETE 事件, 只帶 messageId)
            Broker-->>Member: 廣播給訂閱該站的所有人
        end
    end
```

### 管理公告（ADMIN）

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 管理員
    participant AOP as PreAuthorize AOP
    participant Ctrl as StationChatController
    participant Svc as StationChatService
    participant Metro as MetroService
    participant CDAO as StationChatDAO

    Admin->>AOP: POST /create-announcement
    AOP->>AOP: hasRole('ADMIN')（非管理員 → 403）
    AOP->>Ctrl: 放行
    Ctrl->>Svc: createAnnouncement(dto)
    Svc->>Svc: SecurityContext 取 email → 使用者存在(404)
    Svc->>Metro: existsStationById()（查無 → 404）
    Svc->>CDAO: insertAnnouncement(created_by = 當前管理員, UTC 時間戳)
    Svc-->>Admin: 200 公告新增成功

    Note over Admin,CDAO: 編輯 update-announcement／刪除 delete-announcement 同構：
    Admin->>Ctrl: POST /update-announcement 或 /delete-announcement
    Ctrl->>Svc: updateAnnouncement(dto) / deleteAnnouncement(id)
    Svc->>CDAO: getAnnouncementById()（不濾軟刪的原始查詢）
    Svc->>Svc: Service 層過濾 deletedAt == null（已刪與不存在同回 404）
    alt 查無或已刪
        Svc-->>Admin: 404 StationChatNotFoundException
    else 存在
        Svc->>CDAO: updateAnnouncementContentById() 或 softDeleteAnnouncementById()
        Svc-->>Admin: 200
    end
    Note right of CDAO: 公告為軟刪除、長期存活<br/>與留言「一日生命週期」完全不同
```

### 匯出當日聊天紀錄 Excel（ADMIN，含清理排程的時序耦合）

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 管理員
    participant Ctrl as StationChatController
    participant Svc as StationChatService
    participant Metro as MetroService
    participant CDAO as StationChatDAO
    participant POI as Apache POI
    participant Sched as CleanupScheduler

    Admin->>Ctrl: POST /get-excel-by-station-id（@PreAuthorize ADMIN）
    Ctrl->>Svc: exportMessagesByStationId(stationId)
    Svc->>Metro: getStationNameById()（查無 → 404）
    Svc->>CDAO: getAllMessagesByStationId()（不分頁、舊到新，SQL 無日期過濾）
    Note right of CDAO: 「當日」語意完全依賴清理排程昨日已成功執行
    Svc->>POI: 建表頭（留言id／留言者／類型／內容／建立時間UTC）
    loop 每筆留言
        Svc->>POI: 寫入一列（旅程分享組裝起訖站、轉乘、票價摘要）
    end
    Svc->>POI: 末列寫入「【總計】站名 共 N 則留言」
    POI-->>Svc: byte[]（IOException → StationChatExportExcelFailedException → 500）
    Ctrl-->>Admin: 200 attachment（filename*=utf-8'' RFC 5987 中文檔名）

    Note over Sched,CDAO: 每日 03:00（伺服器時區）
    Sched->>Svc: clearAllMessages()
    Svc->>CDAO: deleteAllMessages()（物理刪除全站留言，含已軟刪）
    Note right of Sched: 整段 try-catch 記 log，單次失敗不影響下次<br/>匯出必須趕在此之前——留言只活一天
```

## 3. 重點摘要（給技術主管）

### 查詢車站訊息與公告

查詢走 HTTP、驗證照常由 `JwtFilter` 把關，會員即可使用。留言查詢的亮點在 Mapper 的共用 `<sql>` 片段：一次 JOIN 使用者、旅程規劃與起訖車站，已被刪除的旅程分享以 `CASE WHEN` 優雅降級為「該旅程分享已被移除」而非破圖；旅程分享的車程時間不是資料庫欄位，由 Service 呼叫捷運模組即時計算，算不出來只記警告、不拖垮整頁回傳，容錯設計得當。公告查詢同構但簡單得多。已知縫隙是 page／size 沿用全專案的未驗證問題，`size=0` 會直接 500。

### 即時聊天（發言與刪言）

這是全系統唯一的 STOMP 路徑，也是第二條驗證路徑：`/ws` handshake 公開，JWT 驗證濃縮在 CONNECT 一個指令、比照 `JwtFilter` 含 Redis 黑名單檢查，通過後 Principal 綁定整條連線。發言有完整檢查鏈——chatType 互斥驗證、分享他人旅程擋 403、每日配額按會員等級查表；刪言做了 `isOwner || isAdmin` 雙軌授權加留言歸站驗證，正是書籤模組刪除所缺的擁有權檢查。**但「只驗 CONNECT 一次」是本模組最重要的審查發現：停權、登出、token 過期都無法終止已建立的連線，被停權的會員只要不斷線就能繼續發言**——`sendMessage` 每次都重載 User 卻不檢查 status，補一行即可堵住。另外通用 STOMP 例外處理直接回傳 `exception.getMessage()`，非預期錯誤會洩漏內部訊息給前端。

### 管理公告

公告管理是管理層 16 個 `@PreAuthorize` 端點中的 4 個（新增、編輯、刪除加匯出），AOP 在方法呼叫前驗角色，是 JWT 之後的第二道防線。新增時 `created_by` 取自 SecurityContext 的當前管理員、不信任前端；編輯與刪除沿用「不濾軟刪的原始查詢＋Service 層判斷已刪」的款式，已刪與不存在同回 404、不洩漏資料痕跡，與書籤模組完全一致。公告走軟刪除、長期存活，和留言的一日生命週期形成同模組內兩種截然不同的資料策略，設計上是刻意為之。

### 匯出當日聊天紀錄 Excel

匯出限 ADMIN，範圍是指定車站不分頁的完整紀錄，POI 組表尾附總計、RFC 5987 處理中文檔名，款式與書籤匯出一致。關鍵在時序耦合：留言每日 03:00 被排程**物理清空**（非軟刪除，含已軟刪資料一併消失），匯出因此是資料歸檔的最後機會，錯過就永久遺失。要注意匯出 SQL 其實沒有日期過濾，「當日」語意完全靠清理排程昨日成功執行來保證——排程失敗的隔天會匯出多日資料；且配額窗按 UTC 午夜、清理按伺服器本地時區 03:00，兩個「一天」並不對齊，自刪留言還能回充每日配額，這三個時間縫建議一併收斂。

# 6. 旅程規劃

## 1. 時序釐清與盲點審查

### 1.1 執行順序與依賴分析

**授權模型：全模組零 `@PreAuthorize`，授權完全是「資料層擁有權」。** 七個端點都落在 `anyRequest().authenticated()` 會員層，HTTP 驗證照常由 `JwtFilter` 每請求把關；角色在本模組沒有意義，取而代之的是兩種資料範圍控制——**列表類查詢天生綁定 scope**（Mapper 一律 `WHERE t.user_id = #{userId}`，userId 從 SecurityContext 的 email 反查，前端無從指定別人），**id 類操作則收斂到單一入口** `getCurrentUserIdAndCheckOwnership()`（`TripPlanService:327`）：SecurityContext email → `UserDAO.getByEmail` → `getActiveOwnerId`（帶 `deleted_at IS NULL`）→ 查無或已軟刪丟 404、非本人丟 403。刪除、改名、更新資訊、匯出四個操作第一步全部呼叫它，這個收斂是本模組最值得講的設計——正是書籤模組 `deleteBookmark` 所缺的那道檢查，在這裡被做成了強制路徑。

**新增鏈（`createTripPlan`，六步依序）**：使用者存在 → 起站存在 → 訖站存在（各自呼叫 `MetroService.existsStationById`）→ 票種/路線策略白名單（`{1,4,5,7}`／`{1,2}`，Service 層常數）→ 會員等級配額查表（`getMaxTripPlansByUserId` JOIN `membership_tiers` + `countActiveByUserId`）→ `insert` 取回自增 id 後 `getById` 回查組 VO。配額的「軟刪回充」在此是**設計內行為**——錯誤訊息明示「請先刪除部分旅程規劃!」，刪除本來就該釋放額度，與聊天室每日配額被自刪回充的漏洞性質完全不同，講的時候要分清楚。

**車程時間是讀取時即時計算、不是資料庫欄位。** 每個回傳 VO 的路徑（新增、列表、單筆、更新後回查）都會呼叫 `enrichTravelTime` → `MetroService.getTravelTimeSecondsByStationIds`，跑一次捷運模組的圖演算法。這帶出本模組的跨模組依賴面：Service 注入 `MetroService` 與 `UserDAO`，DAO 直接查 `membership_tiers` 表（與書籤、聊天室同款式）。

**跨模組生命週期協作（回扣聊天室）**：本模組軟刪除一筆旅程，聊天室的旅程分享 JOIN 條件 `utp.deleted_at IS NULL` 立即失配，歷史訊息即時降級顯示「該旅程分享已被移除」。刪除操作的影響面跨出本模組，但因為聊天室早已做了 `CASE WHEN` 優雅降級，這個耦合是安全的——這是兩模組設計互相成全的正面案例。


### 1.3 亮點與程式風格

**亮點**：擁有權檢查單一入口（`:327`）＋ userId 一律取自 SecurityContext；配額查表化（`membership_tiers.max_trip_plans`，非硬編碼）；Mapper 共用 `<sql>` 片段＋動態 `<if>` 關鍵字＋所有 SELECT 帶 `deleted_at IS NULL`；**Javadoc `@throws` 全數補齊**（每個公開方法都列了完整例外清單，對照聊天室 `getMessages` 的缺漏，本模組是全專案文件紀律最好的一個）；錯誤三類分流乾淨——403（非本人）/404（查無）/400（參數與上限），訊息含具體 id。

**風格一致性問題**：

| #   | 位置                      | 問題                                                                                       | 修改建議                                            |
| --- | ------------------------- | ------------------------------------------------------------------------------------------ | --------------------------------------------------- |
| 1   | `TripPlanController` 全檔 | 8 空格縮排，Service/DAO 為 4 空格（專案通病）                                              | 統一 formatter 設定                                 |
| 2   | `TripPlanService:398`     | `catch (IOException error)` 拋新例外時未帶 cause 也未記 log，錯誤原因遺失（與聊天室同款）  | `logger.error(..., error)` 或建構子帶 cause         |
| 3   | `TripPlanController:219`  | 匯出 200 的 mediaType 誤標 `application/json`，實際回 xlsx（書籤、聊天室同病）             | 改標 spreadsheet MIME                               |
| 4   | `TripPlanService:52,57`   | `VALID_FARE_TYPES`/`VALID_ROUTING_STRATEGIES` 與 `MetroService` 重複定義，註解自承「比照」 | 抽共用常數或 enum，消除漂移風險                     |
| 5   | 兩個 DTO 的 `toString()`  | 皆略去 `notes` 欄位但未註明，易被誤認遺漏（若為控制 log 長度是好意）                       | 加註解說明或補齊                                    |
| 6   | `TripPlanService:219`     | 只有 `deleteTripPlan` 記「通過擁有權驗證」的 debug log，其餘三個 id 操作沒有               | log 收斂進 `getCurrentUserIdAndCheckOwnership` 本體 |
| 7   | `TripPlanService:373`     | try-with-resources 內多餘分號 `new XSSFWorkbook();)`                                       | 移除                                                |
| 8   | 更新類方法                | 更新後 `getById` 回查＋即時車程計算，每次更新 3 個 SQL＋1 次圖計算                         | 可接受，建議註明為刻意換取回傳完整 VO               |

## 2. 各 Use Case 時序圖

### UC_A 新增旅程規劃

```mermaid
sequenceDiagram
    autonumber
    actor M as 會員
    participant JF as JwtFilter
    participant C as TripPlanController
    participant S as TripPlanService
    participant MS as MetroService
    participant UD as UserDAO
    participant D as TripPlanDAO

    M->>JF: POST /api/v1/trip-plan/create-plan
    JF->>C: JWT 驗證通過，寫入 SecurityContext
    C->>C: @Valid 驗 CreateTripPlanDTO（名稱/起訖站/票種/票價/策略必填）
    C->>S: createTripPlan(dto)
    S->>UD: getByEmail(SecurityContext email)
    alt 查無使用者
        S-->>M: UserNotFoundException → 404
    end
    S->>MS: existsStationById(起站)、existsStationById(訖站)
    alt 任一車站不存在
        S-->>M: StationNotFoundException → 404
    end
    S->>S: 票種 ∈ {1,4,5,7}、策略 ∈ {1,2} 白名單
    alt 代碼不合法
        S-->>M: IllegalArgumentException → 400
    end
    S->>D: getMaxTripPlansByUserId（JOIN membership_tiers 查表）
    S->>D: countActiveByUserId（deleted_at IS NULL，軟刪回充額度=設計內）
    alt 已達等級上限
        S-->>M: TripPlanLimitExceededException → 400
    end
    Note over S,D: 檢查與寫入間無交易——並發可繞過上限（TOCTOU）
    S->>D: insert（useGeneratedKeys 取回自增 id）
    S->>D: getById（JOIN 起訖站中文名稱組 VO）
    S->>MS: getTravelTimeSecondsByStationIds（即時 Dijkstra）
    S-->>C: TripPlanVO
    C-->>M: 200 OK
```

### UC_B 查詢旅程規劃

```mermaid
sequenceDiagram
    autonumber
    actor M as 會員
    participant C as TripPlanController
    participant S as TripPlanService
    participant D as TripPlanDAO
    participant MS as MetroService

    rect rgb(235, 245, 255)
    Note over M,MS: 分頁列表 get-all-plan-paginated
    M->>C: keyword?、page、size（未驗證範圍）
    C->>S: getAllTripPlan(keyword, page, size)
    S->>D: getAllPaginatedByUserId（WHERE user_id + deleted_at IS NULL + 動態 LIKE）
    loop 每筆 VO
        S->>MS: getTravelTimeSecondsByStationIds（即時計算）
        Note right of MS: 不容錯——任一筆失配拋 404 拖垮整頁
    end
    S->>D: countAllByUserId
    S-->>M: PaginatedVO（size=0 → 除零＋FETCH NEXT 0 → 500）
    end

    rect rgb(235, 255, 240)
    Note over M,D: 名稱列表 / 名稱模糊搜尋單筆
    M->>C: get-all-plan-name 或 get-plan?keyword=
    C->>S: getAllTripPlanName() / getTripPlanByName(keyword)
    opt keyword 空白
        S-->>M: IllegalArgumentException → 400
    end
    S->>D: getAllNamesByUserId / getLatestByUserIdAndKeyword（同名取最新一筆）
    alt 單筆查無
        S-->>M: TripPlanNotFoundException → 404
    end
    S-->>M: 200 OK（單筆另跑即時車程計算）
    end
```

### UC_C 修改與刪除旅程規劃

```mermaid
sequenceDiagram
    autonumber
    actor M as 會員
    participant C as TripPlanController
    participant S as TripPlanService
    participant UD as UserDAO
    participant D as TripPlanDAO

    M->>C: delete-plan / update-plan-name / update-plan
    C->>C: @Valid 驗 DTO（id 必填）
    C->>S: 對應業務方法
    rect rgb(255, 245, 230)
    Note over S,D: getCurrentUserIdAndCheckOwnership() 單一入口（TripPlanService:327）
    S->>UD: getByEmail(SecurityContext email)
    S->>D: getActiveOwnerId(tripPlanId)（deleted_at IS NULL）
    alt 查無或已軟刪
        S-->>M: TripPlanNotFoundException → 404
    else 非本人
        S-->>M: TripPlanAccessDeniedException → 403
    end
    end
    opt update-plan（更新資訊）
        S->>S: 票種/策略白名單驗證，不合法 → 400
    end
    S->>D: softDeleteById / updateNameById / updateInfoById
    opt 更新類
        S->>D: getById 回查
        S->>S: enrichTravelTime（即時車程計算）
    end
    S-->>M: 200 OK
    Note over D: 軟刪旅程 → 聊天室分享即時降級為「該旅程分享已被移除」（跨模組協作）
```

### UC_D 匯出旅程規劃 Excel

```mermaid
sequenceDiagram
    autonumber
    actor M as 會員
    participant C as TripPlanController
    participant S as TripPlanService
    participant D as TripPlanDAO
    participant POI as Apache POI

    M->>C: POST /get-excel-by-trip-plan-id
    C->>S: exportTripPlanExcel(tripPlanId)
    S->>S: getCurrentUserIdAndCheckOwnership() → 404 / 403
    S->>D: getById（含起訖站中文名稱）
    S->>POI: 單筆單列組表（票種/策略代碼轉中文說明）
    alt IOException
        S-->>M: TripPlanExportExcelFailedException → 500
        Note right of S: catch 未記 cause，錯誤原因遺失
    end
    S-->>C: byte[]
    C-->>M: 200 xlsx（RFC 5987 中文檔名「旅程規劃.xlsx」）
    Note over S,POI: 票價/轉乘次數為前端傳值未經後端驗算，Excel 將其呈現為事實
```

## 3. 重點摘要（給技術主管）

### 新增旅程規劃

新增鏈六步依序把關：使用者存在、起訖站各自存在、票種與路線策略白名單、會員等級配額查表，全部通過才寫入。配額上限存在 `membership_tiers.max_trip_plans`、DAO 查表而非硬編碼，與書籤模組同一套款式；軟刪除會釋放額度，錯誤訊息也明示「請先刪除部分旅程規劃」，這是設計內行為、不是聊天室那種配額回充漏洞。已知縫隙是配額檢查與寫入之間無交易（TOCTOU 並發可超額），且本 Service 全類無 `@Transactional`，對照會員模組五個更新方法都有，值得補齊。

### 查詢旅程規劃

三個查詢端點（分頁列表、名稱列表、名稱模糊搜尋單筆）的 SQL 一律 `WHERE user_id` 綁定當前使用者，資料範圍天生隔離、無越權面。要注意車程時間不是資料庫欄位，而是每筆 VO 讀取時即時呼叫捷運模組跑一次圖演算法——**且這裡的 `enrichTravelTime` 與聊天室的同名方法行為相反，不容錯**：列表中任何一筆旅程的起訖站失配就拋 404 拖垮整頁，加上每頁 8 筆就是 8 次 Dijkstra，建議統一成聊天室的容錯版並評估快取。page/size 未驗證是專案通病第四次出現，`size=0` 直接 500，適合做一次全域收斂。

### 修改與刪除旅程規劃

這是本模組最值得講的設計：刪除、改名、更新資訊、匯出四個指定 id 的操作，第一步全部收斂到同一私有方法 `getCurrentUserIdAndCheckOwnership()`——當前使用者一律取自 SecurityContext、完全不信任前端傳的 id，查無或已軟刪回 404、非本人回 403，錯誤三類分流乾淨且訊息含具體 id。把擁有權檢查做成強制路徑而非各方法自律，正是書籤模組 `deleteBookmark` 缺洞的解法範本，可作為簡報中的正面對照。軟刪除的影響跨出本模組：聊天室的旅程分享會即時降級為「該旅程分享已被移除」，因對方早有 `CASE WHEN` 優雅降級，這個耦合是安全的。

### 匯出旅程規劃 Excel

匯出是會員自助功能（僅限本人），與書籤（全站、無日期壓力）、聊天室（ADMIN、與清理排程時序耦合）形成三種匯出款式的對照組：單筆、有擁有權檢查、無時序耦合。POI 組表把票種與策略代碼轉成中文說明、RFC 5987 處理中文檔名，工整。兩個小縫隙：`catch (IOException)` 未把 cause 記進 log，匯出失敗時查不到原因；以及票價與轉乘次數是前端傳值、後端只驗非負不驗算一致性，Excel 把它們呈現為事實——後端其實有現成的票價計算能力，補一步驗算即可讓匯出資料可信。
