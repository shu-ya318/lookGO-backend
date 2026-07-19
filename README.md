# lookGo - Backend

使用 Spring Boot 3 開發的 lookGo 後端應用程式，提供臺北捷運資訊查詢 REST API，包含使用者認證、車站資料、路線查詢、票價計算、行程規劃等功能，並整合 TDX 運輸資料平台進行資料同步。

## 技術棧

- **框架**：Spring Boot 3.5（Spring MVC、Spring Security）
- **語言**：Java 17
- **建置工具**：Maven（附 Maven Wrapper `mvnw`）
- **ORM**：MyBatis 3.0（`mybatis-spring-boot-starter`）
- **資料庫**：Microsoft SQL Server
- **快取**：Redis（Refresh Token 儲存與資料快取）
- **認證**：Spring Security + JJWT 0.11（Access Token + Refresh Token）
- **驗證**：Jakarta Validation
- **API 文件**：SpringDoc OpenAPI 2.8（Swagger UI）
- **HTTP 客戶端**：Apache HttpClient5（對接 TDX API）
- **容器化**：Docker（multi-stage build）

## API 文件

啟動應用程式後，可透過 Swagger UI 瀏覽並直接測試所有 API 端點：

```
http://localhost:8082/swagger-ui.html
```

- 所有 REST API 前綴為 `/api/v1/<module>`（如 `/api/v1/metro`、`/api/v1/user`、`/api/v1/auth`）
- HTTP 動詞統一採用 `POST`，端點路徑以動詞前綴命名（`get-all-line`、`create-announcement`、`sync-all-station` 等）
- 除白名單路徑（`/api/v1/auth/**`、Swagger 相關路徑）外，其餘端點皆需附上 `Authorization: Bearer <token>` 標頭

## 開始使用

### 環境需求

- JDK 17+（必要）
- Maven 3.9+（可選，或使用內附的 Maven Wrapper）
- Microsoft SQL Server（可本機安裝或使用 Docker）
- Redis（可本機安裝或使用 Docker）
- TDX 運輸資料流通服務平臺的 Client ID / Secret（用於資料同步，可至 [TDX 官網](https://tdx.transportdata.tw/) 申請）

### 安裝

1. **複製專案**

   ```bash
   git clone https://github.com/shu-ya318/lookGo-backend.git
   cd lookGo-backend
   ```

2. **設定環境變數**

   主要設定檔為 `src/main/resources/application.yaml`，預設啟用 `dev` profile（`spring.profiles.active: dev`），開發環境的覆寫設定位於 `src/main/resources/application-dev.yaml`。

   設定檔採用 `${ENV_VAR:defaultValue}` 語法，敏感資訊一律透過環境變數注入：

   | 環境變數 | 說明 | 預設值 |
   |----------|------|--------|
   | `DB_HOST` / `DB_PORT` | SQL Server 主機與連接埠 | `localhost` / `1433` |
   | `DB_NAME` | 資料庫名稱 | — |
   | `DB_USERNAME` / `DB_PASSWORD` | 資料庫帳號密碼 | — |
   | `REDIS_HOST` / `REDIS_PORT` | Redis 主機與連接埠 | `localhost` / `6379` |
   | `REDIS_PASSWORD` | Redis 密碼 | 空字串 |
   | `TDX_CLIENT_ID` / `TDX_CLIENT_SECRET` | TDX API 憑證 | — |
   | `ADMIN_PASSWORD` | 預設管理員帳號密碼 | — |
   | `CORS_ALLOWED_ORIGINS` | 允許的跨域來源 | `http://localhost:5173` |
   | `FRONTEND_BASE_URL` | 前端網址 | `http://localhost:8081` |
   | `RAIL_PROXY_HOST` / `RAIL_PROXY_PORT` | 內網環境對外連線的 Proxy（個人裝置不需設定） | 空 / `0` |

3. **初始化資料庫**

   應用程式啟動時會自動執行 `src/main/resources/schema.sql`（資料庫 DDL）與 `data.sql`（初始資料），無須手動建表；捷運路線、車站、票價等資料可於啟動後透過管理員身分呼叫 `sync-all-*` 系列 API 從 TDX 同步。

### 開發

啟動開發伺服器：

**Windows（CMD / PowerShell）：**

```bash
.\mvnw.cmd spring-boot:run
```

**Unix / Linux / macOS：**

```bash
./mvnw spring-boot:run
```

> **Note:** 應用程式預設運行於 `http://localhost:8082`（由 `application-dev.yaml` 的 `server.port` 設定）。

### 部署

1. **打包成 JAR**

    **Windows：**

    ```bash
    .\mvnw.cmd clean package
    ```

    **Unix / Linux / macOS：**

    ```bash
    ./mvnw clean package
    ```

    > **Note:** 打包完成的 JAR 檔會輸出至 `target/lookGo-backend.jar`；若要跳過測試可加上 `-DskipTests`。

    以 JAR 直接執行：

    ```bash
    java -jar target/lookGo-backend.jar
    ```

2. **容器化部署（Docker）**

    專案採用 multi-stage build，建置時需透過 `SETTINGS_FILE` 參數指定 Maven settings 檔（依網路環境選擇 `settings-public.xml` 或 `settings-nexus.xml`）：

    ```bash
    # 建置 Docker 映像檔
    docker build --build-arg SETTINGS_FILE=settings-public.xml -t lookgo-backend:latest .

    # 執行容器（以 host.docker.internal 連線本機的 SQL Server 與 Redis）
    docker run -d -p 8082:8080 \
      --name lookgo-backend \
      -e DB_HOST=host.docker.internal \
      -e DB_NAME=lookGoDB \
      -e DB_USERNAME=sa \
      -e DB_PASSWORD=YourStrong@Passw0rd \
      -e REDIS_HOST=host.docker.internal \
      -e REDIS_PASSWORD=YourRedisPassword \
      -e TDX_CLIENT_ID=your-tdx-client-id \
      -e TDX_CLIENT_SECRET=your-tdx-client-secret \
      -e ADMIN_PASSWORD=YourAdminPassword \
      lookgo-backend:latest
    ```

    > **Note:**
    > - 容器內應用程式監聽 `8080` 連接埠，可自行調整對外映射（如 `-p 9090:8080`）。
    > - 上述指令假設 SQL Server 與 Redis 運行於宿主機（透過 `host.docker.internal` 連線）；若與資料庫、快取容器部署於同一 Docker network，改以容器名稱作為 `DB_HOST`、`REDIS_HOST` 即可。
    > - 內網環境建置時，`Dockerfile` 已設定 `MAVEN_OPTS` 跳過 SSL 憑證驗證，並可改用 `settings-nexus.xml` 從內部 Nexus 下載套件。

## 專案結構

```
lookGo-backend/
├── Dockerfile
├── pom.xml
├── mvnw / mvnw.cmd
└── src/main/
    ├── java/com/mli/lookgo/
    │   ├── LookGoBackendApplication.java   # 應用程式進入點
    │   ├── core/                           # 跨模組共用的基礎設施
    │   │   ├── config/                     # Spring 設定（SecurityConfig、SwaggerConfig、JacksonConfig）
    │   │   ├── constants/                  # 全域常數
    │   │   ├── controller/                 # 核心控制器（AuthController）
    │   │   ├── dao/                        # 核心資料存取層
    │   │   ├── exceptions/                 # 全域例外與 GlobalExceptionHandler
    │   │   ├── model/dto/                  # 核心請求 DTO
    │   │   ├── result/                     # 共用回應 VO（MessageVO、PaginatedVO）
    │   │   ├── security/                   # JwtFilter、JwtUtil、CookieUtil、UserDetailsServiceImpl
    │   │   └── service/                    # 核心業務層（AuthService、RedisService）
    │   └── module/                         # 功能模組（按領域劃分）
    │       ├── metro/                      # 捷運模組（路線、車站、票價、TDX 同步排程）
    │       │   ├── controller/             # MetroController、MetroSyncController
    │       │   ├── service/                # MetroService、MetroSyncService
    │       │   ├── dao/                    # MetroDAO（MyBatis Mapper 介面）
    │       │   ├── model/                  # entity（DB 實體）、dto（入站）、vo（出站）
    │       │   ├── enums/                  # 模組列舉（RailSystem、StationFacilities）
    │       │   ├── exceptions/             # 模組專屬例外
    │       │   └── scheduler/              # 排程任務（定時同步 TDX 資料）
    │       └── user/                       # 使用者模組（同上結構）
    └── resources/
        ├── application.yaml                # 主設定（datasource、redis、mybatis、logging、rail api）
        ├── application-dev.yaml            # 開發環境覆寫設定
        ├── schema.sql                      # 資料庫 DDL（啟動時執行）
        ├── data.sql                        # 初始資料（啟動時執行）
        └── mappers/                        # MyBatis XML Mapper（AuthMapper.xml、MetroMapper.xml、UserMapper.xml）
```

## 設定說明

### 分層架構

採用 Controller → Service → DAO 三層架構，禁止跨層依賴：

| 層次 | 類別後綴 | 職責 |
|------|----------|------|
| 介面層 | `Controller` | 接收 HTTP 請求、呼叫 Service、回傳 `ResponseEntity` |
| 業務層 | `Service` | 商業邏輯、資料組裝、呼叫 DAO、拋出 Domain 例外 |
| 資料存取層 | `DAO` | MyBatis Mapper 介面，對應 `resources/mappers/*.xml` |

### 資料庫設定

- 使用 Microsoft SQL Server 作為主要資料庫，啟用加密連線（`encrypt=true`）
- MyBatis 啟用 `map-underscore-to-camel-case`，資料庫 `snake_case` 欄位自動對映 Java `camelCase`
- 啟動時自動執行 `schema.sql` 與 `data.sql` 初始化資料表與資料

### 安全性設定

- JWT Token 認證：`JwtFilter` 攔截所有請求驗證 `Authorization: Bearer <token>`
- Access Token 有效期短（開發環境 1 小時）；Refresh Token 存於 Redis（開發環境 7 天），過期後需重新登入
- 角色權限控管：管理員端點以 `@PreAuthorize("hasRole('ADMIN')")` 保護
- 密碼使用 `BCryptPasswordEncoder` 加密儲存
- CORS 跨域支援：允許來源由 `CORS_ALLOWED_ORIGINS` 設定

### Redis 設定

- 儲存 **Refresh Token**，實作登出與 Token 刷新機制
- 支援密碼保護（`REDIS_PASSWORD`）

### TDX 資料同步

- 透過 Apache HttpClient5 對接 [TDX 運輸資料流通服務平臺](https://tdx.transportdata.tw/)，同步捷運路線、車站、票價等資料
- 提供 `sync-all-*` 系列管理員 API 手動全量同步，並由 `scheduler/` 排程任務定時自動同步
- 內網環境可透過 `RAIL_PROXY_HOST` / `RAIL_PROXY_PORT` 設定 Proxy
