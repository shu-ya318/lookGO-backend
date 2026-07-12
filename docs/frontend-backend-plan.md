# lookGo 前後端功能調整全盤實作規劃

> 本文件為八項功能的實作規劃，涵蓋後端（`lookGO-backend`）與前端（`lookGo-frontend`）。
> 各節皆列出：異動檔案、實作步驟、API 契約、UI/UX 設計；**原需求未提及但必須納入的考量已全數採用**，直接併入各節的前、後端規劃，並以括號說明理由。
> 文末附建議實作順序與驗收清單。

---

## 目錄

1. [生日值驗證](#一生日值驗證)
2. [個人頭像修改](#二個人頭像修改)
3. [下拉選單車站搜尋優化](#三下拉選單車站搜尋優化)
4. [匯出檔名加上 username](#四匯出檔名加上-username)
5. [車站書籤與旅程規劃列表排序](#五車站書籤與旅程規劃列表排序)
6. [旅程規劃名稱禁止重複](#六旅程規劃名稱禁止重複)
7. [車站聊天室僅接受文字訊息](#七車站聊天室僅接受文字訊息)
8. [同步票價改為背景執行與輪詢](#八同步票價改為背景執行與輪詢)

---

## 一、生日值驗證

**規則**：以「今日」計算年齡，最小 6 歲（含）、最大 150 歲（含）。
即 `today - 150年 <= birthDate <= today - 6年`。註冊時 birthDate 為選填，**僅在有值時驗證**。

### 1. 後端

#### 異動檔案

| 檔案                                                   | 異動                                   |
| ------------------------------------------------------ | -------------------------------------- |
| `core/validation/BirthDateRange.java`（新增）          | 自定義驗證註解                         |
| `core/validation/BirthDateRangeValidator.java`（新增） | 驗證邏輯實作                           |
| `core/model/dto/SignupDTO.java`                        | `birthDate` 欄位加上 `@BirthDateRange` |
| `module/user/model/dto/UpdateBirthDateDTO.java`        | `birthDate` 欄位加上 `@BirthDateRange` |

#### 實作步驟

1. 在 `core` 新增 `validation` 套件，建立 Jakarta Validation 自定義註解（放在 core 是因為 `SignupDTO`（core）與 `UpdateBirthDateDTO`（user 模組）都要共用）：

```java
/**
 * 驗證出生日期換算年齡是否介於 6 歲（含）至 150 歲（含）之間，null 視為通過（是否必填交由 @NotNull 決定）。
 */
@Documented
@Constraint(validatedBy = BirthDateRangeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface BirthDateRange {
    String message() default "出生日期年齡必須介於 6 歲至 150 歲之間!";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
public class BirthDateRangeValidator implements ConstraintValidator<BirthDateRange, LocalDate> {

    private static final int MIN_AGE = 6;
    private static final int MAX_AGE = 150;

    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return !birthDate.isAfter(today.minusYears(MIN_AGE))
                && !birthDate.isBefore(today.minusYears(MAX_AGE));
    }
}
```

2. `SignupDTO.birthDate`（`SignupDTO.java:38-41`）與 `UpdateBirthDateDTO.birthDate`（`UpdateBirthDateDTO.java:21-25`）加上 `@BirthDateRange`，保留既有 `@PastOrPresent`（年齡下限 6 歲已隱含「不得大於今日」，但保留可回傳更精準的訊息）。
3. 驗證失敗時由既有 `GlobalExceptionHandler.handleMethodArgumentNotValidException` 回傳 400 與欄位錯誤訊息 Map（`{"birthDate": "出生日期年齡必須介於 6 歲至 150 歲之間!"}`），前端無須額外處理格式。
4. Validator 取「今日」一律用 `LocalDate.now(ZoneOffset.UTC)`，如上方程式碼（後端其他時間已統一 UTC（`LocalDateTime.now(ZoneOffset.UTC)`），保持一致可避免伺服器時區在日界線附近的臨界差異）。
5. 測試涵蓋邊界值：今天剛滿 6 歲（`today.minusYears(6)`）與剛滿 150 歲（`today.minusYears(150)`）都必須「合法」，並各測前後一天（規則為含端點的閉區間，邊界最容易寫錯）。
6. 兩個 DTO 的 Swagger `@Schema` `description` 同步補上年齡範圍說明（讓 API 文件與實際驗證行為一致）。

### 2. 前端

#### 異動檔案

| 檔案                                            | 異動                                                                        |
| ----------------------------------------------- | --------------------------------------------------------------------------- |
| `src/utils/validation.ts`                       | 新增 `isValidBirthDateRange`（6–150 歲）；`isValidBirthDate` 移除 1911 檢查 |
| `src/pages/auth/SignupPage.tsx`                 | zod schema 加規則、DatePicker 限制範圍                                      |
| `src/components/user/UpdateBirthDateDialog.tsx` | 同上                                                                        |

#### 實作步驟

1. `validation.ts` 新增（沿用既有 `isValidBirthDate` 的風格，dayjs 已是專案依賴，直接用 dayjs 更簡潔）：

```ts
/** 驗證出生日期換算年齡是否介於 6 歲（含）至 150 歲（含）之間 */
export const isValidBirthDateRange = (value: string): boolean => {
  if (!value) return true; // 是否必填由各表單決定

  const birthDate = dayjs(value);
  const minBirthDate = dayjs().subtract(150, 'year'); // 最老
  const maxBirthDate = dayjs().subtract(6, 'year'); // 最年輕

  return (
    !birthDate.isBefore(minBirthDate, 'day') &&
    !birthDate.isAfter(maxBirthDate, 'day')
  );
};
```

2. 兩個表單的 zod schema 串上 `.refine(isValidBirthDateRange, '出生日期年齡必須介於 6 歲至 150 歲之間!')`（`SignupPage.tsx:49-53`、`UpdateBirthDateDialog.tsx:20-26`）。
3. **UX：從源頭避免選到非法日期** —— 兩處 `<DatePicker>` 把 `disableFuture` 換成明確範圍：

```tsx
minDate={dayjs().subtract(150, 'year')}
maxDate={dayjs().subtract(6, 'year')}
```

使用者用日曆挑選時根本點不到非法日期；手動輸入時則由 zod 規則在 `helperText` 顯示紅字提示（既有 `mode: 'onChange'` 會即時觸發）。

4. 移除 `isValidBirthDate` 內 `year < 1911` 的檢查（1911 早於 150 年前，與新規則完全重疊，留著是永遠不會觸發的死程式碼）。
5. 錯誤訊息前後端統一為「出生日期年齡必須介於 6 歲至 150 歲之間!」一字不差（避免使用者在前端紅字與後端 400 看到兩種說法）。

---

## 二、個人頭像修改

**目標**：`users` 表新增 `avatar` 欄位（可 null、預設為預設頭像 URL）；提供「上傳 base64 圖片（≤1MB、限定格式）」與「移除並恢復預設」兩個操作；前端 Header、聊天室泡泡、設定頁改為顯示 `userInfo.avatar` 並放大尺寸。

### 1. 資料庫（schema.sql + 既有環境的 ALTER 語句）

`schema.sql:35-54` 的 `users` 表新增欄位。base64 data URI（1MB 圖檔約 1.4M 字元）遠超過 `NVARCHAR(4000)` 上限，須用 `NVARCHAR(MAX)`：

```sql
-- schema.sql 的 CREATE TABLE [dbo].[users] 內新增一行（放在 cellphone 之後）
[avatar] NVARCHAR(MAX) NULL CONSTRAINT [DF_users_avatar] DEFAULT N'/assets/default-avatar.png',
```

**既有環境的異動 SQL（依需求提供）**：

```sql
-- 1. 新增 avatar 欄位，允許 NULL，預設值為預設頭像 URL
ALTER TABLE [dbo].[users]
    ADD [avatar] NVARCHAR(MAX) NULL
    CONSTRAINT [DF_users_avatar] DEFAULT N'/assets/default-avatar.png';

-- 2. 將既有使用者補上預設頭像
UPDATE [dbo].[users]
SET [avatar] = N'/assets/default-avatar.png'
WHERE [avatar] IS NULL;
```

> 預設頭像圖檔放前端 `src/assets/default-avatar.png`（或 `public/`），DB 存的是相對路徑字串；「移除頭像」= 把 `avatar` 更新回預設 URL 常數。後端以 `UserConstants.DEFAULT_AVATAR_URL` 常數集中管理這個字串。

### 2. 後端

#### 異動檔案

| 檔案                                                                              | 異動                                                     |
| --------------------------------------------------------------------------------- | -------------------------------------------------------- |
| `module/user/model/entity/User.java`                                              | 新增 `avatar` 欄位                                       |
| `module/user/model/vo/UserVO.java`                                                | 新增 `avatar` 欄位                                       |
| `module/user/model/dto/UpdateAvatarDTO.java`（新增）                              | 接收 base64 圖片字串                                     |
| `module/user/exceptions/InvalidAvatarException.java`（新增）                      | 格式或大小不合法                                         |
| `module/user/service/UserService.java`                                            | 新增 `updateAvatar`、`removeAvatar`                      |
| `module/user/controller/UserController.java`                                      | 新增 `/update-avatar`、`/remove-avatar` 端點             |
| `module/user/dao/UserDAO.java` + `resources/mappers/UserMapper.xml`               | 新增 `updateAvatarById`；所有 SELECT 欄位清單補 `avatar` |
| `core/exceptions/GlobalExceptionHandler.java`                                     | 新增 `InvalidAvatarException` → 400                      |
| `module/stationChat/model/vo/StationChatMessageVO.java` + `StationChatMapper.xml` | 補 `avatar` 欄位與 JOIN `users`（見步驟 6）              |

#### API 契約

```
POST /api/v1/user/update-avatar
Body: { "avatar": "data:image/png;base64,iVBOR..." }
200 → UserVO（含新 avatar）
400 → MessageVO（格式不支援 / 超過 1MB / 非合法 base64）

POST /api/v1/user/remove-avatar
Body: 無
200 → UserVO（avatar 恢復為預設 URL）
```

#### 實作步驟

1. `UpdateAvatarDTO`：單一欄位 `avatar`，`@NotBlank(message = "請上傳頭像圖片!")`。
2. `UserService.updateAvatar` 業務驗證（拋 `AvatarInvalidException`）：
   - 前綴白名單：`data:image/png;base64,`、`data:image/jpeg;base64,`、`data:image/webp;base64,`，否則「僅支援 PNG、JPEG、WEBP 圖片格式!」。
   - `Base64.getDecoder().decode(...)` 失敗 → 「圖片內容不是合法的 base64 編碼!」。
   - 解碼後 byte 長度 > `1 * 1024 * 1024` → 「頭像圖片大小不得超過 1MB!」。
   - （加分項）檢查解碼後檔頭 magic number 與宣告 MIME 是否一致，防止偽造前綴。
3. `UserService.removeAvatar`：直接 `updateAvatarById(userId, DEFAULT_AVATAR_URL, now)`。
4. 兩方法皆沿用專案既有模式：`getAuthenticatedEmail()` → `userDAO.getByEmail` → 更新 → 回傳最新 `UserVO`，並更新 `updated_at`。
5. `GlobalExceptionHandler` 比照 `UserDuplicateException` 加一個 handler，回 400 + `MessageVO`。
6. **聊天室訊息補頭像**：`StationChatMapper.xml` 查訊息的 SQL JOIN `users` 帶出 `avatar`，`StationChatMessageVO` 加 `avatar` 欄位（`MessageBubble.tsx` 顯示的是「訊息發送者」的頭像，資料源是 `StationChatMessageVO` 而非 userStore；若只改 `UserVO`，聊天室裡別人的頭像永遠是預設 icon）。
7. 註冊流程**無需異動** `AuthService`：`AuthMapper.xml:8-13` 的 INSERT 未列出 `avatar` 欄位，DB 的 `DF_users_avatar` DEFAULT 會自動補上預設頭像 URL（預設值單一來源在 DB，程式與 DB 不必兩處維護同一字串，也少改一個檔案）。
8. `User` entity 與 `UpdateAvatarDTO` 的 `toString()` 不印出 base64 全文，截斷為前 30 字元＋總長度（一則 log 夾 1.4M 字元會讓 log 爆量且無法閱讀）。
9. **部署註記**：1MB 圖檔的 base64 JSON 約 1.4MB。Tomcat 對 JSON body 沒有預設限制（`maxPostSize` 只作用於 form data）、`max-swallow-size` 預設 2MB 亦足夠，`application.yaml` 無需異動；但部署至 K8s 且使用 NGINX Ingress 時，需在 Ingress 資源加上 `nginx.ingress.kubernetes.io/proxy-body-size: "2m"` annotation（nginx `client_max_body_size` 預設 1MB，1.4MB 的滿檔上傳會被回 413）。
10. **後續優化項（本次不做）**：base64 存 DB 會讓 `get-current-user` 與聊天訊息查詢的 payload 變大，短期可接受；後續再改「物件儲存（S3/Azure Blob）+ DB 只存 URL」（本次 API 介面存字串，屆時不需改變介面）。

### 3. 前端

#### 異動檔案

| 檔案                                                 | 異動                                                                             |
| ---------------------------------------------------- | -------------------------------------------------------------------------------- |
| `src/services/user/interface.ts`                     | `GetCurrentUserResponse` 加 `avatar: string \| null`；新增 `UpdateAvatarRequest` |
| `src/services/user/index.ts`                         | 新增 `updateAvatar`、`removeAvatar`                                              |
| `src/stores/userStore.ts`                            | 型別自動涵蓋（更新後 `useUserStore.setState({ userInfo })`）                     |
| `src/components/header/UserProfileMenu.tsx`          | `AccountCircle` icon 改為 `<Avatar src={userInfo?.avatar}>`                      |
| `src/components/stationChat/MessageBubble.tsx`       | 兩處 `<Avatar>`（36px）改吃 `message.avatar`，尺寸放大至 44px                    |
| `src/services/stationChat/interface.ts`              | `StationChatMessage` 加 `avatar`                                                 |
| `src/pages/user/SettingPage.tsx`                     | Avatar（80px→120px）改吃 `userInfo.avatar`，加「編輯／移除」入口                 |
| `src/components/user/UpdateAvatarDialog.tsx`（新增） | 上傳/預覽/移除對話框                                                             |
| `src/assets/default-avatar.png`（新增）              | 預設頭像圖檔                                                                     |

#### 實作步驟與 UI/UX 設計

1. **共用 fallback 規則**：`avatar` 為 null 或載入失敗時，顯示預設頭像（`<Avatar src={avatar ?? defaultAvatar}>`，MUI Avatar 的 `src` 載入失敗會自動 fallback 到 children，children 放 `<PersonOutlinedIcon>` 當雙保險）。
2. **Header（`UserProfileMenu.tsx:50`）**：`startIcon={<AccountCircle/>}` 改為 `<Avatar src={...} sx={{ width: 32, height: 32 }}>`（AppBar 高度限制下 32px 是合理放大；目前是 20px 的 small icon）。
3. **聊天室（`MessageBubble.tsx:61-72、185-197`）**：36px → 44px，`src` 改 `message.avatar`。
4. **設定頁（`SettingPage.tsx:136-147`）**：
   - Avatar 80px → 120px，`src={userInfo?.avatar}`。
   - Avatar 右下角疊一顆小型 `IconButton`（`PhotoCameraOutlined`），點擊開啟 `UpdateAvatarDialog`。
   - Dialog 內容：目前頭像預覽 → 「選擇圖片」按鈕（`<input type="file" accept="image/png,image/jpeg,image/webp">` 隱藏 input）→ 選檔後以 `FileReader.readAsDataURL` 轉 base64 並即時預覽 → 「確認上傳」；另提供次要按鈕「移除頭像，恢復預設」。
   - **提示文字**（`Typography variant='caption' color='text.secondary'`）：「支援 PNG、JPEG、WEBP，檔案大小上限 1MB」。
   - **前端先驗**：選檔當下就檢查 `file.size > 1MB` 與 MIME，不合法立即 snackbar 錯誤並清空選擇，不打 API。
5. **狀態同步**：上傳/移除成功後用回傳的 `UserVO` 呼叫 `useUserStore.setState({ userInfo })`（比照 `SettingPage.tsx:50、68` 既有寫法），Header 與設定頁即同步刷新。
6. 上傳中按鈕顯示 loading 並 disable，防重複送出。

---

## 三、下拉選單車站搜尋優化

**前後端皆有異動**。三個改動：後端 `StationOptionVO` 補 `lineColor`；前端選項中的車站代碼改用**帶路線色**的 `<Chip>` 呈現；HomePage 選中即搜尋、移除搜尋按鈕。

### 1. 後端

#### 異動檔案

| 檔案                                         | 異動                                             |
| -------------------------------------------- | ------------------------------------------------ |
| `module/metro/model/vo/StationOptionVO.java` | 新增 `lineColor` 欄位                            |
| `resources/mappers/MetroMapper.xml`          | `getAllStationOption` 補 JOIN `lines` 帶出 color |

#### API 契約

```
POST /api/v1/metro/get-all-station-option（既有端點，回應擴充）
200 → List<StationOptionVO>，每筆新增 lineColor（Hex 字串，如 "#E3002C"）
```

#### 實作步驟

1. `StationOptionVO` 新增 `lineColor` 欄位（`@Schema(description = "路線顏色 (Hex)", example = "#E3002C")`），命名與格式比照既有 `OriginDestinationDetailVO.RouteSegmentVO.lineColor`（`OriginDestinationDetailVO.java:157-158`）；補 getter/setter 並更新 `toString()`。
2. `MetroMapper.xml:254-259` 的 `getAllStationOption` 補 JOIN `lines` 表：

```sql
SELECT ls.station_code, s.name_zh_tw, l.color AS lineColor
FROM [dbo].[lines_stations] ls
INNER JOIN [dbo].[stations] s ON s.id = ls.station_id
INNER JOIN [dbo].[lines] l ON l.id = ls.line_id
ORDER BY ls.line_id ASC, ls.station_sequence ASC
```

3. `MetroDAO`、`MetroService`、`MetroController` 簽章不變（回傳型別同為 `List<StationOptionVO>`），無需異動；Swagger 文件由 `@Schema` 自動更新。
4. 無 DB schema 異動（`lines.color` 已存在：`schema.sql:66`、`NVARCHAR(20) NOT NULL`，資料由 TDX 同步寫入，直接 JOIN 即可）；轉乘站亦無需去重或特殊處理（轉乘站在 `lines_stations` 中每條路線各一列、`station_code` 不同——如台北車站的 R10 與 BL12——各列 JOIN 到自己的路線色，天然正確）。

### 2. 前端

#### 異動檔案

| 檔案                                     | 異動                                      |
| ---------------------------------------- | ----------------------------------------- |
| 定義 `StationOption` 的 interface 檔案   | 新增 `lineColor: string`                  |
| `src/components/StationAutocomplete.tsx` | `renderOption` 改為「名稱 + 路線色 Chip」 |
| `src/pages/HomePage.tsx`                 | 選中自動導頁、移除搜尋 IconButton         |

#### 實作步驟

1. **StationAutocomplete（`StationAutocomplete.tsx:101-113`）**：`renderOption` 不再輸出 `formatStationLabel`（`名稱（CODE）`），改為：

```tsx
renderOption={(props, option) => (
  <li {...props} key={option.stationCode} style={...}>
    <Stack direction='row' sx={{ alignItems: 'center', gap: 1 }}>
      {/*車站中文名稱*/}
      <Typography variant='body2'>{option.nameZhTw}</Typography>
      {/*車站代碼（帶路線色）*/}
      {!isEmptyBookmarkOption(option) && (
        <Chip
          label={option.stationCode}
          size='small'
          sx={{
            fontWeight: 700,
            fontSize: 11,
            bgcolor: normalizeHexColor(option.lineColor),
            color: '#fff',
          }}
        />
      )}
    </Stack>
  </li>
)}
```

樣式與上色方式參考 `StationInfoCard.tsx:119-128` 的 Chip（`size='small'`、粗體、小字、`normalizeHexColor(line.color)` 當背景色）。

- **`getOptionLabel` 維持 `formatStationLabel` 純文字**（它同時是輸入框的顯示值與鍵盤搜尋比對依據，不能塞 JSX；維持「名稱（CODE）」格式讓使用者打「R28」也搜得到）。Chip 只出現在 `renderOption`。
- **lineColor 的 fallback**：`lineColor` 為空或格式異常時，Chip 退回預設灰底（`lines.color` 雖為 NOT NULL 理論上不缺值，但單筆資料異常不可讓整個選項渲染失敗）；書籤下拉的「空書籤」選項沒有路線色，既有 `isEmptyBookmarkOption` 判斷已跳過 Chip，無需處理。

2. **HomePage（`HomePage.tsx:150-178`）**：

- `onChange` 直接執行搜尋：

```tsx
const handleSearch = (station: StationOption | null) => {
  setSelectedStation(station);

  if (station) {
    navigate(`/network-map?search=${encodeURIComponent(station.nameZhTw)}`);
  }
};
```

- 移除 `<IconButton>`（`HomePage.tsx:169-178`）。
- **清除（×）事件不觸發導頁**：`onChange` 收到 `null`（使用者按清除鈕）時只更新 state、不 navigate（上面的 `if (station)` 已涵蓋）。
- 移除按鈕後，輸入框外層圓角白底容器（`HomePage.tsx:140-148`）的右側 padding 微調（原本按鈕佔據的空間消失，不調會視覺失衡）。
- 鍵盤操作（輸入後按 Enter 選中第一個選項）走同一個 `onChange`，行為一致，無需額外處理。

---

## 四、匯出檔名加上 username

**純前端**。username 從 `useUserStore` 的 `userInfo` 讀取。

#### 異動檔案

| 檔案                                                    | 現況                                                        | 改為                                                                    |
| ------------------------------------------------------- | ----------------------------------------------------------- | ----------------------------------------------------------------------- |
| `src/pages/stationBookmark/StationBookmarkPage.tsx:165` | `` `車站書籤_${dayjs().format('YYYYMMDD')}.xlsx` ``         | `` `車站書籤_${username}_${dayjs().format('YYYYMMDD')}.xlsx` ``         |
| `src/components/tripPlan/TripPlanCard.tsx:92`           | `` `${tripPlan.name}_${dayjs().format('YYYYMMDD')}.xlsx` `` | `` `${tripPlan.name}_${username}_${dayjs().format('YYYYMMDD')}.xlsx` `` |
| `src/pages/stationChat/hooks/useExportChatExcel.ts:37`  | 檔名格式同上（無 username）                                 | 檔名一併加入 `username`（與上兩處格式一致）                             |

#### 實作步驟

1. 兩個檔案各自 `const username = useUserStore((state) => state.userInfo?.username ?? '');`。
2. 新增共用工具 `src/utils/file.ts`：

```ts
/** 移除檔名中不合法的字元（Windows 保留字元與控制字元） */
export const sanitizeFilename = (name: string) =>
  name.replace(/[\\/:*?"<>|\x00-\x1f]/g, '_');
```

組檔名時對 `username` 與 `tripPlan.name` 都套用（旅程名稱本來就可能含 `/` 等字元，是既有潛在 bug，一併修掉）。

3. 檔名以 `[前綴, sanitizeFilename(username), dayjs().format('YYYYMMDD')].filter(Boolean).join('_')` 組合，不用模板字串直接串接（路由雖已保護，`userInfo` 仍可能瞬間為 null；fallback 成空字串時直接串接會產生 `車站書籤__20260712.xlsx` 的雙底線）。
4. `useExportChatExcel.ts:37` 的聊天紀錄匯出檔名一併加上 `username`（格式與另外兩處相同，一行改動即可讓全站匯出檔名規則一致）。

---

## 五、車站書籤與旅程規劃列表排序

**目標**：後端 `get-all-*-paginated` 接受排序方向參數（時間新→舊 / 舊→新），前端在搜尋欄旁加排序下拉選單。
排序鍵定義：旅程規劃用 `updated_at`；書籤用 `created_at`（`user_station_bookmarks` 只有 `created_at`，收藏後也沒有可編輯的欄位，「更新時間」在書籤語意上就是「收藏時間」），UI 文案分別寫「更新時間」與「收藏時間」。

### 1. 後端

#### 異動檔案

| 檔案                                                                                  | 異動                                                                     |
| ------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `module/tripPlan/controller/TripPlanController.java:96-105`                           | `get-all-plan-paginated` 加 `sortDirection` 參數                         |
| `module/tripPlan/service/TripPlanService.java:146-160`                                | 傳遞並驗證參數                                                           |
| `module/tripPlan/dao/TripPlanDAO.java:32` + `TripPlanMapper.xml:35`                   | ORDER BY 依參數切換                                                      |
| `module/stationBookmark/controller/StationBookmarkController.java`                    | 同上                                                                     |
| `module/stationBookmark/service/StationBookmarkService.java:139-147`                  | 同上；`getAllBookmark`／`exportBookmarkExcel` 帶入 `userId`（見步驟 4）  |
| `module/stationBookmark/dao/StationBookmarkDAO.java` + `StationBookmarkMapper.xml:33` | 同上；`getAllPaginated`／`getAllActive` 補 `userId` 過濾條件（見步驟 4） |

#### API 契約

```
POST /api/v1/trip-plan/get-all-plan-paginated?keyword=&page=0&size=8&sortDirection=DESC
POST /api/v1/station-bookmark/get-all-bookmark-paginated?keyword=&page=0&size=8&sortDirection=DESC

sortDirection: DESC（預設，新→舊）| ASC（舊→新）
```

#### 實作步驟

1. Controller 加 `@RequestParam(defaultValue = "DESC") String sortDirection`，Swagger `@Parameter` 說明排序鍵（旅程=更新時間、書籤=收藏時間）。
2. **Service 白名單驗證**（重要，見下方注入風險）：

```java
private static final Set<String> VALID_SORT_DIRECTIONS = Set.of("ASC", "DESC");

String normalizedDirection = sortDirection == null ? "DESC" : sortDirection.toUpperCase();
if (!VALID_SORT_DIRECTIONS.contains(normalizedDirection)) {
    throw new IllegalArgumentException("不支援的排序方向: " + sortDirection + "，有效值為 ASC、DESC");
}
```

3. Mapper XML **不用 `${}` 串接**（避免 SQL injection），用 `<choose>` 切換整段 ORDER BY：

```xml
<choose>
    <when test="sortDirection == 'ASC'">
        ORDER BY t.updated_at ASC, t.id ASC
    </when>
    <otherwise>
        ORDER BY t.updated_at DESC, t.id DESC
    </otherwise>
</choose>
```

- 旅程規劃：`TripPlanMapper.xml:35` 現為 `ORDER BY t.created_at DESC`，排序鍵改為 `updated_at`（符合需求「依更新時間」）。
- 書籤：`StationBookmarkMapper.xml:33` 維持 `created_at`，只切換方向。
- 保留 `id` 作第二排序鍵，確保分頁穩定（同一秒多筆時不會跨頁重複/漏資料）。

4. **一併修正書籤未以使用者過濾的問題**：`StationBookmarkService.getAllBookmark`／`exportBookmarkExcel`（`StationBookmarkService.java:139-147、174-180`）目前透過 DAO 的 `getAllPaginated`、`getAllActive` 做**全表查詢**，任何登入者都看得到、匯出得到所有人的書籤（VO 還含 email，屬他人個資外洩）。DAO 與 Mapper 補 `userId` 條件，比照 `TripPlanDAO.getAllPaginatedByUserId` 的既有寫法（與旅程規劃的 per-user 設計一致，也符合第四項「檔名加 username」隱含「匯出的是自己的書籤」的語意）。

### 2. 前端

#### 異動檔案

| 檔案                                                                                       | 異動                                         |
| ------------------------------------------------------------------------------------------ | -------------------------------------------- |
| `src/services/tripPlan/interface.ts`、`src/services/stationBookmark/interface.ts`          | Request 加 `sortDirection?: 'ASC' \| 'DESC'` |
| `src/pages/tripPlan/TripPlanPage.tsx`、`src/pages/stationBookmark/StationBookmarkPage.tsx` | 搜尋欄旁加排序 Select，state 串進 fetch      |

#### 實作步驟與 UI/UX 設計

1. 兩頁各加 state：`const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('DESC');`。
2. 在 `SearchInput` 旁（同一列 Stack 內、搜尋欄右側）放小型 `Select`：

```tsx
<Select
  size='small'
  value={sortDirection}
  onChange={(e) => {
    setPage(0);
    setSortDirection(e.target.value);
  }}
>
  <MenuItem value='DESC'>更新時間：新 → 舊</MenuItem>
  <MenuItem value='ASC'>更新時間：舊 → 新</MenuItem>
</Select>
```

（書籤頁文案用「收藏時間」。）

3. `fetchAllTripPlan` / `fetchAllStationBookmark` 與 `handleLoadMore` 的請求都帶上 `sortDirection`；`sortDirection` 加入 `useCallback` 依賴，變更時自動重抓第 0 頁（比照 `keyword` 的既有模式；切換排序必須重置 `page=0` 整批重抓，否則「載入更多」會出現新舊排序混排）。
4. 有 `keyword` 時將排序 Select 設為 `disabled`（兩頁在有關鍵字時走的是「單筆查詢」API（`getTripPlan`、`getStationBookmarkByStationName`），只回一筆、排序無意義，disabled 避免使用者誤以為排序壞掉）。
5. RWD：該列已是 `justifyContent: space-between`，小螢幕允許換行（`flexWrap: 'wrap', gap: 1`，避免搜尋欄與 Select 擠在同一行溢出）。

---

## 六、旅程規劃名稱禁止重複

**目標**：同一使用者的有效（未軟刪除）旅程不得同名。`createTripPlan` 與 `updateTripPlanName` 在呼叫 DAO 寫入前檢查，違反拋 `TripPlanNameDuplicationException`；前端顯示後端錯誤訊息，並在兩個對話框加 info 色提示行。

### 1. 後端

#### 異動檔案

| 檔案                                                                       | 異動                                                         |
| -------------------------------------------------------------------------- | ------------------------------------------------------------ |
| `module/tripPlan/exceptions/TripPlanNameDuplicationException.java`（新增） | 繼承 `RuntimeException`，比照專案範本                        |
| `module/tripPlan/dao/TripPlanDAO.java` + `TripPlanMapper.xml`              | 新增 `existsActiveByUserIdAndName`                           |
| `module/tripPlan/service/TripPlanService.java`                             | `createTripPlan`（:89）與 `updateTripPlanName`（:237）加檢查 |
| `core/exceptions/GlobalExceptionHandler.java`                              | 新增 handler → 409                                           |
| `resources/schema.sql`                                                     | 新增 filtered unique index（見步驟 5）                       |

#### 實作步驟

1. DAO 新增：

```java
/**
 * 檢查指定使用者是否已有同名的有效（未軟刪除）旅程規劃，可排除指定 id（更新名稱時排除自身）。
 *
 * @param userId
 * @param name
 * @param excludeId 要排除的旅程規劃 id，新增情境傳 null
 * @return 是否存在同名旅程
 */
boolean existsActiveByUserIdAndName(@Param("userId") Integer userId,
        @Param("name") String name,
        @Param("excludeId") Integer excludeId);
```

XML 採 EXISTS 寫法：`SELECT CAST(CASE WHEN EXISTS (SELECT 1 FROM user_trip_plans WHERE user_id = #{userId} AND name = #{name} AND deleted_at IS NULL <if test="excludeId != null">AND id != #{excludeId}</if>) THEN 1 ELSE 0 END AS BIT)`（EXISTS 找到一筆即返回、不必計數掃描，且回傳 BIT 可直接對應 DAO 的 boolean）。

2. `createTripPlan`：在數量上限檢查之前加：

```java
if (tripPlanDAO.existsActiveByUserIdAndName(user.getId(), createTripPlanDTO.getName().trim(), null)) {
    throw new TripPlanNameDuplicationException("已有名稱為「" + createTripPlanDTO.getName().trim() + "」的旅程規劃，請改用其他名稱!");
}
```

3. `updateTripPlanName`：`existsActiveByUserIdAndName(userId, name.trim(), tripPlanId)` —— **排除自身**，讓「存回原名」視為合法（等同未變更）。注意 `getCurrentUserIdAndCheckOwnership` 已回傳 userId，可直接沿用。
4. `GlobalExceptionHandler` 新增 handler，回 **409 Conflict**（「資源狀態衝突」的主流語意，重複名稱屬此類；不沿用 `BookmarkDuplicateException` 的 400——400 語意是請求格式錯誤——並於 handler Javadoc 註明本例外固定回 409）：

```java
@ExceptionHandler(TripPlanNameDuplicationException.class)
public ResponseEntity<MessageVO> handleTripPlanNameDuplicationException(
        TripPlanNameDuplicationException exception) {
    logger.error("旅程規劃名稱重複: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageVO(exception.getMessage()));
}
```

5. **DB 防線（防競態條件）**：兩個並發請求可能同時通過 service 檢查而各自寫入重複資料，於 `schema.sql` 加 SQL Server filtered unique index（filtered 條件讓軟刪除的資料不佔名額）：

```sql
CREATE UNIQUE NONCLUSTERED INDEX [UQ_user_trip_plans_user_id_name_active]
ON [dbo].[user_trip_plans] ([user_id], [name])
WHERE [deleted_at] IS NULL;
```

違反索引時 DAO 拋 `DataIntegrityViolationException`，在 `GlobalExceptionHandler` 轉譯成與步驟 4 相同的 409 訊息（service 預檢負責友善訊息、索引負責併發下的最終一致性，兩層各司其職）。

6. **比較規則**：名稱一律先 `trim()` 再比對與寫入（步驟 2、3 的程式碼已套用）；SQL Server 預設 collation 不分大小寫，「Taipei」與「taipei」視為重複（此行為合理，於 Swagger 描述註明即可）。錯誤訊息帶出重複的名稱（讓使用者不用回列表比對，步驟 2 的文案已涵蓋）。

### 2. 前端

#### 異動檔案

| 檔案                                                   | 異動                             |
| ------------------------------------------------------ | -------------------------------- |
| `src/components/tripPlan/TripPlanEditorDialog.tsx`     | 旅程名稱欄位標籤下加 info 提示行 |
| `src/components/tripPlan/UpdateTripPlanNameDialog.tsx` | 同上                             |

#### 實作步驟與 UI/UX 設計

1. 兩個對話框在 `FormLabel`（`UpdateTripPlanNameDialog.tsx:100-109` 與 EditorDialog 對應處）之下、輸入框之上加一行常駐提示：

```tsx
<Typography variant='caption' sx={{ color: 'info.main', mb: 0.5 }}>
  名稱不能與既有的旅程規劃重複
</Typography>
```

使用 MUI theme 的 `info` 色（依需求），`variant='caption'` 維持視覺層級低於欄位標籤。

2. 錯誤顯示：兩個對話框的 submit `catch` 已用 `enqueueSnackbar((error as string) || ...)` 顯示後端訊息，409 的 `MessageVO.message`（「已有名稱為…」）會直接呈現，無需額外改動——但需確認 `services/api.ts` 的攔截器對 409 也是取 `message` 欄位丟出（與 400 相同處理即可）。
3. **即時前端預檢**：兩個對話框開啟時以既有 `getAllTripPlanName` API 抓一次名稱清單，zod `.refine` 即時顯示「此名稱已存在」紅字（利用既有 API 與既有 zod 模式、無新增後端成本，使用者不必等送出才知道重複；後端檢查與 DB 索引仍是最終防線）。更新名稱對話框的重複判定需排除「原名」（存回原名視為合法，與後端排除自身的邏輯一致）。
4. `TripPlanEditorDialog` 也負責「編輯」模式（更新 info 不改名），提示行與預檢只在名稱欄位可編輯的情境顯示（不可編輯時顯示提示只會造成困惑）。

---

## 七、車站聊天室僅接受文字訊息

### 現況判定（需求要求先判斷）

**目前寫法「無法」完全禁止 base64 圖片**：

- `SendMessageDTO.content` 只有 `@Size(max = 1000)`（`SendMessageDTO.java:23`）；`StationChatService.sendMessage` 只檢查 TEXT 型別非空白與不得帶 `tripPlanId`（`StationChatService.java:241-247`）。
- 一張小圖的 data URI（如 `data:image/png;base64,iVBOR...`）若在 1000 字元內即可通過並存入 DB，前端若未過濾就可能被當內容渲染。
- 另注意：**發送訊息走 STOMP**（`/station-chat/{stationId}/send-message`），不是 REST；錯誤是經 `StationChatStompController` 的 `@MessageExceptionHandler` 送到 `/user/queue/errors`，不會經過 `GlobalExceptionHandler`。`GlobalExceptionHandler` 仍要加 handler（防未來 REST 化 & 統一風格），但**前端收錯誤的通道是 STOMP error queue**。

### 1. 後端

#### 異動檔案

| 檔案                                                                     | 異動                |
| ------------------------------------------------------------------------ | ------------------- |
| `module/stationChat/exceptions/InvalidChatContentException.java`（新增） | 非文字內容例外      |
| `module/stationChat/service/StationChatService.java:241-247`             | TEXT 分支加內容驗證 |
| `core/exceptions/GlobalExceptionHandler.java`                            | 新增 handler → 400  |

#### 實作步驟

1. 在 `sendMessage` 的 TEXT 分支（空白檢查之後）加驗證，拋 `InvalidChatContentException("聊天室僅接受純文字訊息!")`，規則：

```java
/** data URI 前綴（不分大小寫），如 data:image/png;base64,xxxx */
private static final Pattern DATA_URI_PATTERN =
        Pattern.compile("(?i)data:[\\w.+-]+/[\\w.+-]+;base64,");
/** 連續 200 字元以上的 base64 字元序列，攔截未帶前綴的裸 base64 */
private static final Pattern LONG_BASE64_PATTERN =
        Pattern.compile("[A-Za-z0-9+/=]{200,}");
/** HTML 標籤，如 <img src=...>、<script> */
private static final Pattern HTML_TAG_PATTERN =
        Pattern.compile("(?i)<\\s*(img|script|iframe|svg|object|embed)\\b");
```

命中任一 pattern 即拒絕。（正常中文/英文聊天內容幾乎不可能出現 200 字連續 base64 字元集，誤殺率極低。）

2. **驗證位置**：放在 TEXT 分支的空白檢查之後、「每日則數上限」檢查**之前**（避免非法內容白白消耗使用者的每日發言額度）。定位上這是內容政策而非安全邊界（真正的 XSS 防護仍靠前端 React 的預設轉義——`MessageBubble` 以 `{message.content}` 渲染純文字——本驗證是資料品質防線）。
3. `GlobalExceptionHandler` 加 `InvalidChatContentException` → 400 + `MessageVO`（依需求檔案清單，同時防未來 REST 化時漏接）。
4. STOMP 路徑不需要新 handler：`StationChatStompController.handleException`（:135-141）已將任何例外的 message 包成 `MessageVO` 回送 `/user/queue/errors`，前端即會顯示這則繁中訊息。

### 2. 前端

#### 異動檔案

| 檔案                                                | 異動                 |
| --------------------------------------------------- | -------------------- |
| `src/components/stationChat/MessageSection.tsx:224` | placeholder 文案更新 |

#### 實作步驟

1. 需求寫 `StationChatPage.tsx`，但輸入框實際在 **`MessageSection.tsx:220-232`**（StationChatPage 只傳 state 下來）。將：

```tsx
placeholder={selectedStation ? '請輸入訊息...' : '請先選擇車站'}
```

改為：

```tsx
placeholder={selectedStation ? '請輸入內容（僅接受文字訊息）' : '請先選擇車站'}
```

2. 前端在送出前跑與後端相同的 data URI 檢查，命中直接 snackbar 提示（與後端同一條 regex、一行檢查，省一次 STOMP 來回；後端驗證仍是最終防線）。
3. **驗證錯誤顯示鏈路實測**：確認 `src/pages/stationChat/hooks` 內的 STOMP hook 有訂閱 `/user/queue/errors` 並以 snackbar 顯示 `MessageVO.message`（刪除/驗證錯誤已走這條路，理論上已存在，但新錯誤訊息上線前實測一次最保險）。

---

## 八、同步票價改為背景執行與輪詢

**目標**：`syncAllStationFare` 觸發後立即回 **202 Accepted** 開始背景同步；新增查詢狀態 API 供前端每 30 秒輪詢；狀態回應含**進度百分比**；前端顯示階段訊息並在斷線/登出時中止輪詢。

### 1. 後端

#### 異動檔案

| 檔案                                                           | 異動                                                           |
| -------------------------------------------------------------- | -------------------------------------------------------------- |
| `module/metro/model/vo/SyncStatusVO.java`（新增）              | 狀態回應 VO                                                    |
| `module/metro/enums/SyncStatusEnum.java`（新增）               | `IDLE / RUNNING / SUCCESS / FAILED`                            |
| `module/metro/service/StationFareSyncStateHolder.java`（新增） | 執行緒安全的進度狀態容器（@Component 單例）                    |
| `module/metro/service/MetroSyncService.java:404-458`           | 拆出 `startSyncAllStationFare` 啟動方法，原同步邏輯移至 Worker |
| `module/metro/service/StationFareSyncWorker.java`（新增）      | `@Async` 背景執行原 `syncAllStationFare` 邏輯＋進度回報        |
| `module/metro/controller/MetroSyncController.java:138-143`     | 改回 202；新增 `/sync-all-station-fare/status`                 |
| `module/metro/scheduler/MetroSyncScheduler.java:66`            | 改呼叫 `startSyncAllStationFare`（見步驟 7）                   |
| `LookGoBackendApplication.java`（或新 config）                 | `@EnableAsync` 與自訂 `ThreadPoolTaskExecutor`                 |

#### API 契約

```
POST /api/v1/metro/sync/sync-all-station-fare
202 → MessageVO("已開始背景同步票價資料，請透過狀態查詢 API 追蹤進度!")
409 → MessageVO("票價同步正在進行中，請勿重複觸發!")   ← 併發防護

POST /api/v1/metro/sync/sync-all-station-fare/status
200 → SyncStatusVO:
{
  "status": "RUNNING",          // IDLE | RUNNING | SUCCESS | FAILED
  "progressPercentage": 63,      // 0-100 整數；IDLE 為 0
  "message": "票價資料批次寫入中 (63%)",
  "startedAt": "2026-07-12T03:21:00Z",
  "finishedAt": null             // SUCCESS / FAILED 時有值
}
```

#### 實作步驟

1. **`StationFareSyncStateHolder`**：以 `AtomicReference<SyncStatusEnum>` + `AtomicInteger progress` + volatile 時間戳實作；提供 `tryStart()`（`compareAndSet(非RUNNING → RUNNING)`，回傳 false 表示已有同步在跑）、`updateProgress(int)`、`markSuccess()/markFailed(String)`、`snapshot()`。單機部署下 in-memory 即可；**若未來多實例部署需改存 Redis**（專案已有 `RedisService`，預留介面即可，本次不做）。
2. **Service 拆分**：
   - `startSyncAllStationFare()`：呼叫 `stateHolder.tryStart()`，失敗拋新例外 `SyncInProgressException`（→ GlobalExceptionHandler 409）；成功則呼叫 async 方法後立即返回。
   - `@Async("metroSyncExecutor") doSyncAllStationFare()`：搬移原 `syncAllStationFare` 邏輯，try/catch 全包——成功 `markSuccess()`、任何例外 `markFailed(訊息)` 並 log（背景執行緒的例外不會傳到任何 HTTP 回應，**必須**自行捕捉）。
   - **注意 Spring 代理限制**：`@Async` 方法不可由同類別內部呼叫（self-invocation 不經代理、註解會失效）。async 方法放在獨立的 `StationFareSyncWorker` @Component（獨立類別讓代理必然生效，職責切分也更清晰，是 Spring 社群的主流做法）。
   - **`@Transactional` 調整**：原方法整段 `@Transactional` 會讓數十萬筆票價在一個長交易裡。移除方法級 `@Transactional`，讓每批 `metroDAO.upsertAllStationFare` 呼叫各自自動提交（不必另包 batch 方法，改動最小；進度真實反映已提交資料、失敗時已完成批次不回滾，upsert 可重跑、冪等）。
3. **進度計算**（兩階段加權，避免長時間卡 0%）：
   - 階段一「向 TDX 抓取票價」（`fetchAllStationFare`，受 API rate limit、最耗時）：佔 0–70%。以「已抓取的請求數 / 總請求數」回報。
   - 階段二「批次寫入 DB」（`MetroSyncService.java:446-453` 既有迴圈）：佔 70–100%。每批寫完 `updateProgress(70 + 30 * totalInserted / totalSize)`。
   - message 同步更新為「正在向 TDX 取得票價資料 (35%)」/「票價資料批次寫入中 (85%)」等，前端可直接顯示。
4. **Controller**：
   - `syncAllStationFare()` 改 `ResponseEntity.accepted().body(new MessageVO(...))`，Swagger `@ApiResponse` 改 202，補 409。
   - 新增 `/sync-all-station-fare/status`，同樣 `@PreAuthorize("hasRole('ADMIN')")`，回 `SyncStatusVO`。沿用專案慣例用 `@PostMapping`。
5. `GlobalExceptionHandler` 加 `SyncInProgressException` → 409。
6. **執行緒池**：`@EnableAsync` 搭配自訂 `ThreadPoolTaskExecutor`（bean 名 `metroSyncExecutor`），設 `corePoolSize=1, maxPoolSize=1, queueCapacity=0`（專用單執行緒池天然保證同時只有一個票價同步在跑，與 `tryStart()` 形成雙保險，也不影響 Spring 預設 executor 上的其他任務）。
7. **排程共用同一入口**：`MetroSyncScheduler.java:66` 現在直接呼叫 `syncAllStationFare()`，改為呼叫 `startSyncAllStationFare`（排程與手動觸發共用同一個狀態容器與併發防護，避免兩者並發執行同一份同步）。排程觸發時若 `tryStart()` 失敗，記 log 略過本輪即可。
8. **伺服器重啟**：in-memory 狀態遺失、重啟後 status 回 IDLE，屬可接受行為（upsert 冪等，管理者重按一次即可；單機部署不值得為此引入 Redis 狀態持久化），於 status API 的 Swagger 描述註明。

### 2. 前端

#### 異動檔案

| 檔案                                        | 異動                                                                                               |
| ------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| `src/services/metroSync/index.ts`           | `syncAllStationFare` 移除 15 分鐘 timeout；新增 `getStationFareSyncStatus`；兩者支援 `AbortSignal` |
| `src/components/admin/MetroSyncSection.tsx` | 票價項目改為「觸發 → 顯示已開始 → 每 30 秒輪詢 → 完成/失敗提示」                                   |

#### 實作步驟與 UI/UX 設計

1. **services**：

```ts
export const syncAllStationFare = async (
  signal?: AbortSignal,
): Promise<ApiResponse> =>
  postRequest<ApiResponse>('/metro/sync/sync-all-station-fare', {}, { signal });

export const getStationFareSyncStatus = async (
  signal?: AbortSignal,
): Promise<StationFareSyncStatus> =>
  postRequest<StationFareSyncStatus>(
    '/metro/sync/sync-all-station-fare/status',
    {},
    { signal },
  );
```

`StationFareSyncStatus` interface 對應後端 `SyncStatusVO`。2. **MetroSyncSection 輪詢流程**（票價項目專屬，其他五項維持現行同步模式）：

- 按下同步 → 呼叫 `syncAllStationFare`，**202 成功時先 snackbar「已開始背景同步票價資料」**（第一支 API 的成功訊息，依需求）。
- 啟動輪詢：`setInterval` 每 **30 秒**呼叫 `getStationFareSyncStatus`；啟動時也立即打一次，不等第一個 30 秒。
- 狀態呈現：`RUNNING` → 票價列顯示「同步中…（63%）」＋ `LinearProgress` 進度條（用後端 `progressPercentage`，比純文字更直觀）；按鈕維持 loading/disabled。`SUCCESS` → 停止輪詢、snackbar 成功。`FAILED` → 停止輪詢、snackbar 顯示後端 message。
- 409（已在同步中）→ 不當錯誤處理，直接進入輪詢模式接手顯示進度。

3. **中斷訊號（AbortController）**：

```ts
const abortRef = useRef<AbortController | null>(null);
// 開始輪詢時：abortRef.current = new AbortController();
// 每次請求傳入 abortRef.current.signal
// 清理：clearInterval + abortRef.current?.abort()
```

中止時機：(a) `useEffect` cleanup——元件卸載（含登出導頁、切換頁面）；(b) 收到 401（登出/逾期）時，axios 攔截器讓請求失敗，輪詢 catch 到即 `abort` + 停止。這即是需求的「因斷線或登出等非預期因素中斷時，發起中斷請求的訊號」。

- **斷線容錯**：單次輪詢失敗（網路抖動）不立即放棄，連續失敗 3 次（≈90 秒）才停止輪詢並提示「無法取得同步狀態，請重新整理後再試」。

4. **頁面重進恢復**：元件 mount 時先打一次 status API，若為 `RUNNING` 直接進入輪詢模式——重新整理或重登後仍能接續顯示進度（這也讓 `beforeunload` 警告對票價項目不再必要，`MetroSyncSection.tsx:74-85` 的攔截可排除票價、僅保留給其他仍為同步請求的五個項目）。
5. 「同步時間較長，請耐心等候」的 note（:46）改為「背景同步約需數分鐘，期間可離開此頁」。

---

## 建議實作順序與提交切分

| 順序 | 內容                               | 理由                                                  | commit 範例                                                                |
| ---- | ---------------------------------- | ----------------------------------------------------- | -------------------------------------------------------------------------- |
| 1    | 功能一（生日驗證）                 | 純驗證、無 schema 異動、風險最低                      | `feat: 新增出生日期年齡範圍驗證`                                           |
| 2    | 功能七（聊天內容驗證）             | 後端小改 + 一行文案                                   | `feat: 車站聊天室僅接受文字訊息`                                           |
| 3    | 功能六（名稱防重複）               | 含 DB 索引，供功能五之前先穩定 tripPlan 模組          | `feat: 旅程規劃名稱禁止重複`                                               |
| 4    | 功能五（排序）＋書籤 user 過濾修正 | 兩模組對稱改動一次做完                                | `feat: 車站書籤與旅程規劃列表支援排序`                                     |
| 5    | 功能三、四                         | 功能三後端僅擴充 VO 與一個 JOIN，風險低；功能四純前端 | `feat: 車站搜尋選中即查詢並顯示路線色代碼`、`feat: 匯出檔名加入使用者名稱` |
| 6    | 功能二（頭像）                     | 含 schema 異動與跨模組（stationChat VO），改動面最大  | `feat: 新增個人頭像上傳與顯示`                                             |
| 7    | 功能八（背景同步）                 | 引入 @Async 基礎設施，獨立驗證                        | `feat: 同步票價改為背景執行並提供進度查詢`                                 |

## 驗收清單（摘要）

- [ ] 生日：註冊/修改選 5 歲、151 歲被擋（前端紅字 + 後端 400）；剛滿 6 歲與剛滿 150 歲當天通過。
- [ ] 頭像：上傳 1MB 檔前端即擋；上傳 PNG 成功後 Header、設定頁、聊天室（含他人視角）同步顯示；移除後恢復預設圖。
- [ ] 首頁：下拉選中站名立即導向路網圖；按清除鈕不導頁；選項中代碼以帶路線色的 Chip 呈現（如 R28 紅底、BL12 藍底），API 回應每筆含 `lineColor`。
- [ ] 匯出：書籤與旅程 xlsx 檔名含 username；含特殊字元的旅程名稱可正常下載。
- [ ] 排序：切換新→舊/舊→新後列表重載且「載入更多」順序正確；非法 `sortDirection` 回 400。
- [ ] 旅程名稱：建立同名回 409 且 snackbar 顯示「已有名稱為…」；改名存回原名成功；兩對話框顯示 info 色提示行。
- [ ] 聊天室：貼上 `data:image/png;base64,...` 被拒且收到錯誤訊息；placeholder 顯示「請輸入內容（僅接受文字訊息）」。
- [ ] 票價同步：觸發後立即收到 202；進度條隨輪詢更新；重複觸發回 409 並接手顯示進度；重新整理頁面後 RUNNING 狀態可恢復輪詢；登出後輪詢中止（Network 面板無後續請求）。
