# 車站管理功能開發規劃

前端車站管理頁面所需功能：
1. 顯示所有車站清單，支援分頁與關鍵字模糊搜尋
2. 下拉選單選中單一車站，過濾查詢該車站資料
3. 對單一車站更新 id 以外的屬性（單一 dialog 顯示所有屬性，支援一次改多個欄位，僅送出有異動的欄位）

---

## 一、資料表修改

因決定開放 `nameZhTw` 可編輯，`stations` 表新增不可變欄位 `original_name_zh_tw NVARCHAR(100) NOT NULL`，做為 `MetroDAO.upsertAllStation` 同步比對鍵與 `MetroSyncService.syncAllLineStation` 站名對照用，任何管理端 API 皆不可寫入。`schema.sql` 以 `IF NOT EXISTS (SELECT 1 FROM sys.columns ...)` 判斷欄位是否存在，若不存在則 `ALTER TABLE` 新增、以既有 `name_zh_tw` backfill 後再鎖 `NOT NULL`，維持 idempotent 執行慣例。

- 是否需要為關鍵字模糊搜尋新增索引（例如 `name_zh_tw`、`name_en` 的 nonclustered index）：目前車站筆數為捷運全路網等級（約 100～300 筆），`LIKE '%keyword%'` 全表掃描效能無虞，**建議暫不新增索引**。

---

## 二、Java 程式碼開發

### (一) (可選) Config

不需修改 `SecurityConfig` / `SecurityConstants`。新增的管理端點皆不加入 `SecurityConstants.API_PUBLIC_ALL`，沿用專案既有慣例：預設 `.anyRequest().authenticated()`，並在 Controller 方法加上 `@PreAuthorize("hasRole('ADMIN')")`（與 `UserController#getAllUser`、`UserController#updateStatus`、`StationChatController` 系列端點作法一致）。`@EnableMethodSecurity` 已於現有設定啟用，免異動。

### (二) API

#### 1. 整體待確認清單，並提供建議作法

| 項目 | 說明 | 建議作法 |
|---|---|---|
| `nameZhTw` 是否開放編輯 | `MetroDAO.upsertAllStation` 的 MERGE 語法原以 `name_zh_tw` 作為比對鍵，若開放編輯會導致下次同步比對不到原名稱、誤插入重複車站，且既有以 `station.id` 建立關聯的資料（`station_fares`、`lines_stations`、使用者收藏、行程規劃、聊天室訊息/公告）仍指向舊的那筆列，造成資料分裂。 | 新增不可變欄位 `stations.original_name_zh_tw`（建站當下由 TDX 資料寫入一次，任何管理端 API 皆不可寫入），`upsertAllStation` 的 `ON` 條件與 `syncAllLineStation` 的站名比對皆改用此欄位；`name_zh_tw` 因此與比對鍵脫鉤，改為單純顯示欄位，**開放於 `UpdateStationDTO` 編輯**。 |
| 設施欄位是否也會被排程覆蓋 | 會。`MetroSyncService` 會定期以 data.taipei 開放資料（`fetchAllStationFacility`）透過 `ISNULL(source, target)` 回填 `atm`/`nursingRoom`/`elevator` 等欄位；只要該次外部資料集對該站有提供值就會覆蓋管理員手動修改的內容，僅當外部資料為 null 時才保留手動值。 | 本次先依需求開發 CRUD 功能；請使用者知悉手動修正可能於下次排程後被覆蓋。若要讓管理員的修改具「鎖定、不被同步覆蓋」的效果，需另外設計欄位鎖定機制，屬於後續優化，**不列入本次範圍**。 |
| 單站查詢的回應是否需要獨立 VO | 現有 `getAllStation()` 端點即直接回傳 `List<Station>` entity，非額外包一層 VO。 | `get-station-by-id` **直接複用既有 `Station` entity** 作為回應內容，欄位已具備完整 `@Schema` 標註，不需重複建立與 entity 雷同的 VO。 |
| 下拉選單資料來源 | 既有 `getAllStationOption()` / `StationOptionVO` 以 `stationCode` 為鍵（來自 `lines_stations`），同一車站橫跨多路線時會重複出現多筆；車站管理操作的對象是 `stations.id`，兩者不通用。 | 新增一支以 `stations` 表為主、`id + nameZhTw` 為內容的端點與新 VO `StationIdOptionVO`，避免下拉選單重複選項，並讓前端直接以 `id` 呼叫查詢/更新 API。 |
| 分頁列表要不要回傳完整設施文字 | 各設施欄位為 `NVARCHAR(1000)`，若列表每列都回傳完整內容，分頁資料量會偏大。 | 列表新增輕量 VO `StationVO`（`id`/`nameZhTw`/`nameEn`/`updatedAt`），選中後另呼叫單站查詢取得完整屬性供 dialog 顯示。 |
| 更新時所有欄位皆為 null（僅傳 id）是否視為錯誤 | 若不擋下，會產生一筆只更新 `updated_at`、無任何實質異動的寫入。 | Service 層檢查 `UpdateStationDTO` 除 `id` 外全部欄位皆為 null 時，丟出 `IllegalArgumentException("請至少提供一個要修改的欄位!")`（沿用 `GlobalExceptionHandler` 既有的 400 通用處理）。 |
| 是否需要新例外類別 | `StationNotFoundException` 已存在且對應 404。 | 單站查詢與更新的「找不到車站」情境皆**沿用既有 `StationNotFoundException`**，不需新增例外類別，`GlobalExceptionHandler` 免修改。 |

#### 2. 依開發順序列出各 API

開發順序：先建立無寫入風險的查詢端點（下拉選項 → 分頁列表 → 單站查詢），最後才開發風險最高、且需新引入動態 SQL 寫法的更新端點。

| API（依開發順序） | (1) Model 層 | (2) 資料存取層 | (3) 業務邏輯層 | (4) Controller 層 |
|---|---|---|---|---|
| **① 取得車站下拉選項清單**<br>`POST /api/v1/metro/get-all-station-id-option` | 無需 DTO（無輸入參數）。新增 VO `StationIdOptionVO`（`module/metro/model/vo`）：`id`（Integer）、`nameZhTw`（String），比照 `StationOptionVO` 寫法（無 Lombok，明確 getter/setter + `toString()`）。 | `MetroDAO` 新增 `List<StationIdOptionVO> getAllStationIdOption()`；`MetroMapper.xml` 新增對應 `<select>`：`SELECT id, name_zh_tw FROM [dbo].[stations] ORDER BY id ASC`（直接查 `stations` 表，非 `lines_stations`，避免跨路線重複）。 | `MetroService` 新增 `getAllStationIdOption()`，單純轉呼叫 DAO 並回傳，無例外情境。 | `MetroController` 新增 `getAllStationIdOption()` 方法，`@PreAuthorize("hasRole('ADMIN')")`、`@PostMapping("/get-all-station-id-option")`，回傳 `ResponseEntity<List<StationIdOptionVO>>`，Swagger 標註比照既有 `getAllStationOption` 寫法。 |
| **② 取得所有車站分頁列表**<br>`POST /api/v1/metro/get-all-station-paginated` | 無需 DTO，沿用 `@RequestParam`（`keyword`、`page`、`size`，比照 `UserController#getAllUser`）。新增 VO `StationVO`（`module/metro/model/vo`）：`id`、`nameZhTw`、`nameEn`、`updatedAt`（輕量列表欄位）。 | `MetroDAO` 新增 `List<Station> getAllStationPaginated(@Param("keyword") String, @Param("offset") int, @Param("limit") int)`、`long countAllStation(@Param("keyword") String)`；`MetroMapper.xml` 新增對應 `<select>`，比照 `UserMapper.xml` 的 `getAllPaginated` / `countAll`：`<where><if test="keyword != null and keyword != ''">name_zh_tw LIKE CONCAT('%', #{keyword}, '%') OR name_en LIKE CONCAT('%', #{keyword}, '%')</if></where>`、`ORDER BY id ASC`、`OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY`。 | `MetroService` 新增 `getAllStation(String keyword, int page, int size)`，組裝 `PaginatedVO<StationVO>`：呼叫 DAO 取得分頁資料與總筆數、`totalPages = (int) Math.ceil((double) totalElements / size)`、entity 轉 `StationVO`，比照 `UserService#getAllUser` 寫法。 | `MetroController` 新增 `getAllStation(keyword, page, size)` 方法，`@PreAuthorize("hasRole('ADMIN')")`、`@PostMapping("/get-all-station-paginated")`，參數以 `@RequestParam` 接收（`keyword` 選填、`page`/`size` 有預設值），回傳 `ResponseEntity<PaginatedVO<StationVO>>`。 |
| **③ 依 id 查詢單一車站詳細資料**<br>`POST /api/v1/metro/get-station-by-id` | 新增 DTO `StationIdDTO`（`module/metro/model/dto`）：`id`（Integer，`@NotNull(message = "請輸入車站id!")`），比照 `StationDetailsDTO` 的驗證註解風格。回應**直接複用既有 `Station` entity**（見待確認清單第 3 點），不新增 VO。 | 沿用既有 `MetroDAO.existsById(Integer)`。另新增 `Optional<Station> getById(@Param("stationId") Integer stationId)`；`MetroMapper.xml` 新增對應 `<select>`：`SELECT id, name_zh_tw, name_en, atm, nursing_room, diaper_table, charging_station, ticket_machine, locker, drinking_water, restroom, elevator, escalator, updated_at FROM [dbo].[stations] WHERE id = #{stationId}`。 | `MetroService` 新增 `getStationById(StationIdDTO dto)`：呼叫 `metroDAO.getById(dto.getId())`，`.orElseThrow(() -> new StationNotFoundException("找不到id:" + dto.getId() + "的車站!"))`，比照 `getStationByCode` 的例外拋出風格。 | `MetroController` 新增 `getStationById(@Valid @RequestBody StationIdDTO dto)` 方法，`@PreAuthorize("hasRole('ADMIN')")`、`@PostMapping("/get-station-by-id")`，回傳 `ResponseEntity<Station>`。 |
| **④ 更新單一車站資料（部分欄位）**<br>`POST /api/v1/metro/update-station` | 新增 DTO `UpdateStationDTO`（`module/metro/model/dto`）：`id`（Integer，`@NotNull`）；`nameZhTw`（`@Size(max = 100)`，選填，見待確認清單，實際同步比對鍵為 `original_name_zh_tw`，不受此欄位影響）；`nameEn`（`@Size(max = 200)`，選填）；`atm`/`nursingRoom`/`diaperTable`/`chargingStation`/`ticketMachine`/`locker`/`drinkingWater`/`restroom`/`elevator`/`escalator`（皆為 `String`，選填，`@Size(max = 1000)`，對應 DB 欄位長度）。 | 沿用既有 `MetroDAO.existsById(Integer)` 做存在性檢查。新增 `void updateStationById(@Param("dto") UpdateStationDTO dto, @Param("updatedAt") LocalDateTime updatedAt)`；`MetroMapper.xml` 新增 `<update>`，首次引入 `<set><if test="dto.field != null">column = #{dto.field},</if></set>` 動態欄位更新寫法（現有 codebase 僅有 MERGE-ISNULL 批次寫法與固定單欄位更新，如 `StationChatMapper#updateAnnouncementContentById`，此為新增的動態部分更新慣例）：`UPDATE [dbo].[stations] <set>...(逐欄位 <if>)... updated_at = #{updatedAt}</set> WHERE id = #{dto.id}`。 | `MetroService` 新增 `updateStation(UpdateStationDTO dto)`：① 檢查 `metroDAO.existsById(dto.getId())`，不存在則丟出 `StationNotFoundException("找不到id:" + dto.getId() + "的車站!")`；② 檢查除 `id` 外欄位皆為 null 時丟出 `IllegalArgumentException("請至少提供一個要修改的欄位!")`；③ 呼叫 `metroDAO.updateStationById(dto, LocalDateTime.now(ZoneOffset.UTC))`，比照 `UserService` 更新方法由 Service 層帶入 `updatedAt` 的慣例；④ 回傳 `new MessageVO("車站資料更新成功!")`。 | `MetroController` 新增 `updateStation(@Valid @RequestBody UpdateStationDTO dto)` 方法，`@PreAuthorize("hasRole('ADMIN')")`、`@PostMapping("/update-station")`，回傳 `ResponseEntity<MessageVO>`；Swagger `@ApiResponses` 需涵蓋 200/400/401/403/404/500，比照 `UserController#updateStatus` 寫法。 |
