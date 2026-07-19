# 捷運模組重構規劃

## 已完成

### 票價欄位的缺值語意與同站票價修正

> 範圍：`getOriginDestinationDetail` 的 `farePrice` 欄位（`MetroService`、`MetroRouteGraphService`、`OriginDestinationDetailVO`）
>
> 完成日期：2026.07.19

#### 一、確立 `farePrice` 的缺值語意：維持 null，不轉為 0

`farePrice` 在兩種情況下為 null，兩者語意不同：

| 情境 | 條件                                  | 語意                                         |
| ---- | ------------------------------------- | -------------------------------------------- |
| A    | 呼叫端未傳入 `fareType`               | **未請求**：本次查詢不需要票價，故不查資料庫 |
| B    | 有傳入 `fareType`，但資料庫查無該組合 | **未知**：票價資料缺漏                       |

由於 VO 會回聲 `fareType`，前端可自行區分兩者：`fareType` 為 null 即情境 A，`fareType` 有值而 `farePrice` 為 null 即情境 B。

**決策：維持回傳 null，不改為 0。** 理由是 0 元在票價領域是合法的業務值，若拿它當「未知」的哨兵值（sentinel value），會使「這段行程 0 元」與「不知道這段行程多少錢」在型別上無法區分，資訊永久遺失。改為 0 並沒有消除缺值，只是把它偽裝起來，使錯誤從「前端漏判 null 而顯示空白」惡化為「前端毫不知情地顯示假數字」。

**前端對應規範：null 應渲染為佔位符（`—` 或「票價資料暫無」），不得渲染為 0**，否則會對使用者呈現事實錯誤的金額。情境 A 通常整個票價區塊都不應出現。

另註：專案未設定全域 `NON_NULL`（`JacksonConfig` 僅調整日期與 enum），故 `farePrice` 會明確輸出 `"farePrice": null` 而非省略欄位。這是刻意保留的行為——明確的 null 優於欄位消失，後者會與「舊版 API 無此欄位」混淆。

#### 二、`@Schema` 補上 `nullable = true`

`OriginDestinationDetailVO.farePrice` 原本僅在描述文字提及「未指定票種時為 null」，缺少機器可讀標註，前端以 OpenAPI 產生 TypeScript 型別時會得到 `number` 而非 `number | null`，在型別層面就漏掉了缺值情況。

```java
@Schema(description = "票價 (元；未指定票種、或指定票種但查無票價資料時為 null)", example = "45.00", nullable = true)
private BigDecimal farePrice;
```

描述文字一併補上情境 B（原文只涵蓋情境 A）。

#### 三、查無票價時記錄 WARN

情境 B 原本在後端完全靜默，維運上看不見資料缺口。於 `MetroService.getOriginDestinationDetail()` 查詢票價後新增：

```java
// 有指定票種卻查無票價，代表票價資料缺漏（同步未執行或來源缺該組合），需留下紀錄供維運追查
if (fareType != null && farePrice == null) {
        logger.warn("查無票價資料，fromStationCode: {}，toStationCode: {}，fareType: {}",
                        fromCode, toCode, fareType);
}
```

此日誌與票價同步的稽核需求相呼應：若大量出現，通常代表票價同步失敗或從未執行，可作為排查起點。

#### 四、同站手續費依票種區分

原 `MetroRouteGraphService.SAME_STATION_FARE` 為單一常數 20 元，且**不隨票種變動**——學生、兒童、愛心票在起訖同站時都被錯誤計為 20 元。實際費率為全票 20 元、其餘優惠票種 8 元。

拆分為兩個常數，並新增私有方法集中判斷：

```java
// 起訖同站（進出同一車站）的手續費：全票 20 元，其餘優惠票種（學生、兒童、愛心）一律 8 元
public static final BigDecimal SAME_STATION_FULL_FARE = BigDecimal.valueOf(20);
public static final BigDecimal SAME_STATION_CONCESSION_FARE = BigDecimal.valueOf(8);

// 全票票種代碼，用於區分同站手續費適用的費率
private static final int FULL_FARE_TYPE = 1;

private BigDecimal resolveSameStationFare(Integer fareType) {
    if (fareType == null) {
        return null;
    }

    return fareType == FULL_FARE_TYPE ? SAME_STATION_FULL_FARE : SAME_STATION_CONCESSION_FARE;
}
```

`buildSameStationResult()` 改呼叫 `resolveSameStationFare(fareType)`，維持「未傳入 `fareType` 時不計算票價」的既有語意。

> 採「全票以外一律 8 元」的判斷方式，而非逐一列舉 4、5、7，因票種合法性已於 `MetroService` 入口以 `VALID_FARE_TYPES` 驗證，此處不重複把關；未來若新增優惠票種且費率相同，無需再改動此方法。

#### 驗收狀態

- `mvn compile` 通過
- 起訖同站且 `fareType=4/5/7` 時，`farePrice` 應為 8；`fareType=1` 時應為 20
- 指定票種但查無票價時，回應中 `farePrice` 為 null 且日誌出現對應 WARN
- 前端配合：確認 null 渲染為佔位符而非 0

---

## TODO

### 票價背景同步的韌性與稽核

> 範圍：`module/metro` 的票價背景同步（`StationFareSyncWorker`、`StationFareSyncStateHolder`、`MetroSyncService`、`MetroSyncController`）
>
> 撰寫日期：2026.07.19

---

#### 一、背景與問題陳述

現行票價同步採「非同步任務 + 狀態輪詢」模式：管理員呼叫 `POST /api/v1/metro/sync-all-station-fare` 後立即取得 202，實際工作交由 `metroSyncExecutor` 單執行緒池執行，前端輪詢 `/sync-all-station-fare/status` 追蹤進度。

此架構本身符合主流實踐——**權限檢查發生在「提交任務」的瞬間，任務一旦被接受，其擁有者即為系統而非使用者 session**。因此管理員登出、關閉瀏覽器、甚至 token 過期，都不會中斷同步，這是預期且正確的行為（與 GitHub Actions 觸發後登出、CI 仍跑完同理）。

但在此前提下，現行實作存在三個缺口：

| #   | 缺口                                             | 影響                                                 | 優先級 |
| --- | ------------------------------------------------ | ---------------------------------------------------- | ------ |
| 1   | 執行緒被中斷時，不完整的同步會被標記為 `SUCCESS` | **資料正確性**：部分資料被誤判為全量同步成功         | P0     |
| 2   | 無任何觸發者紀錄                                 | **稽核缺失**：無法回溯是誰、或是排程觸發了同步       | P1     |
| 3   | 無取消機制                                       | **可維運性**：誤觸發後只能等待數十分鐘或重啟應用程式 | P2     |

---

#### 二、缺口一：中斷後誤判為成功（P0）

##### 現況分析

`fetchAllStationFare()` 有兩處 `Thread.sleep()`，捕捉 `InterruptedException` 後皆採「回設中斷旗標 → 提前結束 → 正常回傳」的處理方式：

- `StationFareSyncWorker.java:195-202`：初始等待 60 秒被中斷 → `return allResult`（此時為**空清單**）
- `StationFareSyncWorker.java:243-249`：頁間等待 20 秒被中斷 → `break`（此時為**部分資料**）

問題在於，方法正常 return 後 `runSync()` 會毫無察覺地繼續執行：

- 若為空清單 → 走到 `if (stationFares.isEmpty())` 印出 warn 後 `return`
- 若為部分資料 → **將不完整的票價資料 upsert 進資料庫**

兩種情況最終都會回到 `doSyncAllStationFare()` 的 try 區塊尾端，呼叫 `stateHolder.markSuccess()`（`StationFareSyncWorker.java:80`），前端因而看到「票價資料同步成功!」與 100% 進度。

##### 觸發情境

正常登出不會 interrupt 執行緒，因此日常操作踩不到。實際風險場景是**應用程式關閉**：`ThreadPoolTaskExecutor` 在 context 關閉時預設會對工作執行緒發出 interrupt，此時若同步正在頁間等待，就會寫入部分資料並標記成功。容器化部署（K8s rolling update、Docker restart）會頻繁觸發此路徑。

##### 重構方案

**設計原則**：中斷是「任務未完成」的訊號，必須讓例外向上傳播，由統一的 catch 區塊判定為失敗，而非在低層默默吞掉。

###### 步驟 1：新增中斷專用的等待方法

在 `StationFareSyncWorker` 新增私有方法，將 `InterruptedException` 轉為 unchecked 例外向上拋：

```java
/**
 * 等待指定毫秒數，若執行緒在等待期間被中斷則回設中斷旗標並拋出例外，
 * 使同步流程中止並由 {@link #doSyncAllStationFare()} 判定為失敗，
 * 避免不完整的資料被誤標記為同步成功。
 *
 * @param milliseconds 等待毫秒數
 * @param phase        當前階段描述，用於日誌
 * @throws SyncInterruptedException 等待期間被中斷時拋出
 */
private void sleepOrInterrupt(int milliseconds, String phase) {
    try {
        Thread.sleep(milliseconds);
    } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new SyncInterruptedException("票價同步於「" + phase + "」被中斷");
    }
}
```

###### 步驟 2：新增 `SyncInterruptedException`

於 `module/metro/exceptions/` 建立，遵循專案既有例外規範：

```java
/**
 * 背景同步執行緒於等待期間被中斷時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.19
 */
public class SyncInterruptedException extends RuntimeException {
    public SyncInterruptedException(String message) {
        super(message);
    }
}
```

> **不需**在 `GlobalExceptionHandler` 註冊 handler。此例外僅在背景執行緒內流動，永遠不會傳播到任何 HTTP 請求執行緒，加了也不會被觸發。此處與 `SyncInProgressException`（由 HTTP 請求執行緒拋出、需轉 409）性質不同，這點值得在 code review 時特別說明。

###### 步驟 3：替換兩處 sleep

```java
// 原本 195-202 行
sleepOrInterrupt(STATION_FARE_INITIAL_WAIT_MS, "初始等待");

// 原本 243-249 行
sleepOrInterrupt(PAGE_INTERVAL_MS, "第 " + pageNumber + " 頁後的頁間等待");
```

移除原本的 try-catch 與 `return allResult` / `break`，`fetchAllStationFare()` 因而簡化。

###### 步驟 4：在 `doSyncAllStationFare()` 分流處理

中斷與一般失敗應給出不同訊息，方便維運判讀：

```java
@Async("metroSyncExecutor")
public void doSyncAllStationFare() {
    logger.debug("背景執行緒開始同步票價資料");
    try {
        runSync();
        stateHolder.markSuccess();
        logger.debug("票價資料背景同步完成");
    } catch (SyncInterruptedException exception) {
        logger.warn("票價資料背景同步被中斷: {}", exception.getMessage());
        stateHolder.markFailed("票價資料同步被中斷，資料可能不完整，請重新執行同步");
    } catch (Exception exception) {
        logger.error("票價資料背景同步失敗", exception);
        stateHolder.markFailed("票價資料同步失敗");
    }
}
```

> **順帶修正**：現行 `StationFareSyncWorker.java:83` 寫成 `logger.error("...失敗", exception.getMessage())`。SLF4J 的雙參數多載中，第二參數若非 `Throwable` 會被當成 `{}` 佔位符的填充值，但訊息中並無 `{}`，導致**堆疊追蹤完全遺失**。應傳入 `exception` 本身。

###### 步驟 5：設定 executor 的優雅關閉

於 `MetroSyncAsyncConfig.metroSyncExecutor()` 補上：

```java
executor.setWaitForTasksToCompleteOnShutdown(true);
executor.setAwaitTerminationSeconds(30);
```

語意為：關閉時先給執行中的任務 30 秒收尾，逾時才強制中斷。票價同步動輒數十分鐘，30 秒不足以跑完，因此仍會被中斷——但此時步驟 1–4 已確保它被正確標記為 `FAILED` 而非 `SUCCESS`。此設定的真正價值在於讓「正在寫入資料庫的那個批次」有機會完成，避免交易被硬切。

###### 驗收標準

- 同步進行中關閉應用程式，重啟後查詢狀態不應出現 `SUCCESS`
- 單元測試：mock `Thread.sleep` 拋出 `InterruptedException`，驗證 `stateHolder` 收到 `markFailed` 且 `metroDAO.upsertAllStationFare` 未被呼叫
- 檢查中斷後執行緒的 interrupt 旗標仍為 `true`（未被吞掉，符合 Java 併發慣例）

---

#### 三、缺口二：缺少觸發者稽核紀錄（P1）

##### 設計考量

同步任務不隸屬於使用者 session，但「誰觸發的」必須留下紀錄——這是稽核（audit）需求，與生命週期管理是兩件事，不可混為一談。

關鍵細節：`startSyncAllStationFare()` 有**兩個呼叫來源**：

1. `MetroSyncController.syncAllStationFare()` — 由管理員觸發，`SecurityContext` 中有 authentication
2. `MetroSyncScheduler.syncAllDataPipeline()` — 由排程觸發（`MetroSyncScheduler.java:72`），`SecurityContext` **為空**

因此取得觸發者的邏輯必須容忍 `null`，不能直接照抄其他 Service 的 `SecurityContextHolder.getContext().getAuthentication().getName()`（該寫法在排程執行緒中會 NPE）。

##### 重構方案

###### 步驟 1：`MetroSyncService` 新增觸發者解析

```java
/**
 * 取得本次同步的觸發者名稱，供稽核記錄使用。
 * 排程觸發時 SecurityContext 為空，回傳固定識別字串。
 *
 * @return 觸發者名稱，排程觸發時為 "SYSTEM_SCHEDULER"
 */
private String resolveTriggeredBy() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
        return "SYSTEM_SCHEDULER";
    }

    return authentication.getName();
}
```

###### 步驟 2：`tryStart()` 接收觸發者

```java
public synchronized boolean tryStart(String triggeredBy) {
    if (status.get() == SyncStatusEnum.RUNNING) {
        return false;
    }

    status.set(SyncStatusEnum.RUNNING);
    progressPercentage.set(0);
    message = "已開始背景同步票價資料...";
    startedAt = LocalDateTime.now(ZoneOffset.UTC);
    finishedAt = null;
    this.triggeredBy = triggeredBy;

    return true;
}
```

搭配新增 `private volatile String triggeredBy;` 欄位。

###### 步驟 3：`startSyncAllStationFare()` 串接並記錄

```java
public void startSyncAllStationFare() {
    String triggeredBy = resolveTriggeredBy();

    if (!stationFareSyncStateHolder.tryStart(triggeredBy)) {
        throw new SyncInProgressException("票價同步正在進行中，請勿重複觸發!");
    }

    logger.info("票價同步啟動，觸發者: {}", triggeredBy);
    stationFareSyncWorker.doSyncAllStationFare();
}
```

> 依專案日誌規範，「重要事件、資料同步」屬 `INFO` 層級，故此處用 `logger.info` 而非現行的 `logger.debug`。同步的起訖是維運上最需要留存的事件。

###### 步驟 4：`SyncStatusVO` 增加欄位

```java
@Schema(description = "本次同步的觸發者帳號，排程觸發為 SYSTEM_SCHEDULER", example = "admin@example.com")
private String triggeredBy;
```

一併更新建構子、getter / setter 與 `toString()`。

> **安全性檢查**：`triggeredBy` 存放的是帳號識別（username），非密碼或 token，可安全輸出至日誌與 API 回應。但此端點本身已受 `@PreAuthorize("hasRole('ADMIN')")` 保護，不會外洩給一般使用者。

###### 驗收標準

- 管理員手動觸發後查詢狀態，`triggeredBy` 顯示其帳號
- 排程觸發後，`triggeredBy` 為 `SYSTEM_SCHEDULER` 且不拋 NPE
- 日誌中可見完整的「啟動（含觸發者）→ 結束（成功／失敗）」事件對

---

#### 四、缺口三：無取消機制（P2）

##### 設計考量

**必須明確拒絕的做法**：把「登出」當成隱式取消訊號。這會使任務生命週期與 token 效期耦合，導致長時間同步在 token 過期時被意外終止——正是本文件開頭所述架構要避免的反模式。

正確做法是提供**顯式**的取消端點，同樣受 ADMIN 權限保護。取消是一個獨立的、有意圖的管理動作。

實作上有兩種選擇：

| 方案                     | 做法                                                                     | 評估                                                                          |
| ------------------------ | ------------------------------------------------------------------------ | ----------------------------------------------------------------------------- |
| A. 協作式旗標            | StateHolder 持有 `volatile boolean cancelRequested`，worker 在迴圈中檢查 | ✅ **採用**。語意明確、可在安全點（批次邊界）停止、不需改動 `@Async` 回傳型別 |
| B. `Future.cancel(true)` | `doSyncAllStationFare()` 改回傳 `Future<Void>`，保存後呼叫 cancel        | 依賴 interrupt 語意，停止點不可控，且需額外管理 Future 參考                   |

採方案 A，並與缺口一的機制共用同一套「中止即失敗」的處理路徑。

##### 重構方案

###### 步驟 1：`SyncStatusEnum` 新增狀態

```java
CANCELLED("CANCELLED", "同步已取消"),
```

取消與失敗語意不同——前者是人為決定，後者是非預期錯誤，前端應以不同樣式呈現，不應混用 `FAILED`。

###### 步驟 2：`StationFareSyncStateHolder` 新增取消支援

```java
private volatile boolean cancelRequested;

/**
 * 請求取消目前進行中的同步。
 * 僅設定旗標，實際中止由背景執行緒在下一個安全點（分頁或批次邊界）自行完成。
 *
 * @return 是否成功送出取消請求（false 表示目前無同步進行中）
 */
public synchronized boolean requestCancel() {
    if (status.get() != SyncStatusEnum.RUNNING) {
        return false;
    }

    cancelRequested = true;
    message = "已收到取消請求，正在中止同步...";

    return true;
}

/**
 * 供背景執行緒查詢是否已被請求取消。
 *
 * @return 是否已請求取消
 */
public boolean isCancelRequested() {
    return cancelRequested;
}

/**
 * 標記同步已被取消，保留取消當下的進度值。
 */
public void markCancelled() {
    message = "票價同步已取消，資料可能不完整";
    finishedAt = LocalDateTime.now(ZoneOffset.UTC);
    status.set(SyncStatusEnum.CANCELLED);
}
```

`tryStart()` 中須加入 `cancelRequested = false;` 重置，避免上次的取消旗標殘留影響新一輪同步。**此點極易遺漏，是本項重構最主要的缺陷風險。**

###### 步驟 3：worker 於安全點檢查

在兩個迴圈的**開頭**檢查（而非結尾），確保剛送出取消請求就能在下一輪立即生效：

```java
// fetchAllStationFare() 的 while 迴圈開頭
if (stateHolder.isCancelRequested()) {
    throw new SyncCancelledException("票價同步於分頁請求階段被取消");
}

// runSync() 的批次寫入 for 迴圈開頭
if (stateHolder.isCancelRequested()) {
    throw new SyncCancelledException("票價同步於批次寫入階段被取消");
}
```

新增 `SyncCancelledException`（同 `SyncInterruptedException` 的建立方式），並於 `doSyncAllStationFare()` 加上對應 catch：

```java
} catch (SyncCancelledException exception) {
    logger.info("票價資料背景同步被取消: {}", exception.getMessage());
    stateHolder.markCancelled();
}
```

> **取消點的時效說明**：頁間等待為 20 秒，因此取消請求最長需 20 秒才生效（等待結束、進入下一輪迴圈開頭才被檢查到）。這是可接受的延遲，API 回應訊息應明確告知「已送出取消請求」而非「已取消」，避免前端誤判。若要即時中止，需讓 `sleepOrInterrupt` 改為可被喚醒的等待（如 `CountDownLatch.await(timeout)`），但複雜度提升且效益有限，本次不採用。

###### 步驟 4：新增取消端點

於 `MetroSyncController`，路徑沿用既有 `/sync-all-station-fare/{動作}` 的子路徑風格（與 `/status` 一致）：

```java
@Operation(summary = "取消票價同步", description = "送出取消請求以中止進行中的票價背景同步，背景執行緒將於下一個安全點（最長約 20 秒）中止。僅限 ADMIN 角色存取")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "已送出取消請求", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class))),
        @ApiResponse(responseCode = "401", description = "存取token無效或已過期", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "未授權錯誤，token無效或已過期"))),
        @ApiResponse(responseCode = "403", description = "權限不足", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "權限不足，無法操作!"))),
        @ApiResponse(responseCode = "409", description = "目前無票價同步進行中", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageVO.class, example = "目前無票價同步進行中，無法取消!"))),
        @ApiResponse(responseCode = "500", description = "伺服器內部錯誤", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class, example = "伺服器端錯誤!"))) })
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/sync-all-station-fare/cancel")
public ResponseEntity<MessageVO> cancelSyncAllStationFare() {
    logger.debug("收到取消票價同步的請求");
    MessageVO apiResult = metroSyncService.cancelSyncAllStationFare();

    return ResponseEntity.ok(apiResult);
}
```

Service 層對應方法在無同步進行中時，拋出既有的 `SyncInProgressException` 並不恰當（語意相反）。建議新增 `SyncNotRunningException` 並於 `GlobalExceptionHandler` 註冊為 409，或評估直接回傳帶說明的 200——**此處需在實作前確認前端期望的行為**。

###### 驗收標準

- 同步進行中呼叫取消端點，狀態於 20 秒內轉為 `CANCELLED`，且已寫入的資料保持完整（不做 rollback，因採 upsert 具冪等性）
- 取消後立即重新觸發同步，能正常啟動（驗證 `cancelRequested` 已重置）
- 無同步進行中時呼叫取消端點，回應明確且不影響狀態

---

#### 五、缺口四：多實例部署與重啟韌性（暫不實作）

`StationFareSyncStateHolder.java:14-17` 的類別註解已正確指出：目前為單機 in-memory 設計，多實例部署需改用 Redis。此處補充完整的演進路徑，供未來評估：

| 演進階段 | 觸發條件                     | 技術選型                                                                                               |
| -------- | ---------------------------- | ------------------------------------------------------------------------------------------------------ |
| 現況     | 單機部署                     | in-memory `AtomicReference` + 單執行緒池雙保險                                                         |
| 階段一   | 水平擴展至多實例             | 狀態移至 Redis（專案已有 `RedisService`）；分散式鎖用 `SETNX` + TTL，或引入 ShedLock 保護 `@Scheduled` |
| 階段二   | 需要重啟後續跑、失敗自動重試 | Spring Batch（內建 JobRepository 持久化、chunk 交易、restart）                                         |
| 階段三   | 同步任務數量與種類增長       | 訊息佇列（RabbitMQ / Kafka）解耦觸發與執行                                                             |

**目前規模不建議提前引入階段二以後的方案**：票價同步為每週一次的單一任務，upsert 天然冪等，失敗後重跑成本可接受，導入 Spring Batch 的複雜度遠高於收益。

唯一需留意的近期風險：若專案在缺口一修正前就進行多實例部署，`tryStart()` 的 synchronized 僅在單一 JVM 內有效，兩個實例可能同時對 TDX 發起分頁請求，**觸發 API 速率限制導致雙方皆失敗**。因此「多實例部署」與「Redis 分散式鎖」必須綁定同一次上線。

---

#### 六、實作順序與影響範圍

建議依序執行，每項獨立提交：

| 順序 | 項目                      | 異動檔案                                                                         | 破壞性                              |
| ---- | ------------------------- | -------------------------------------------------------------------------------- | ----------------------------------- |
| 1    | 中斷判定為失敗 + 修正日誌 | `StationFareSyncWorker`、`MetroSyncAsyncConfig`、新增 `SyncInterruptedException` | 無（純內部行為修正）                |
| 2    | 觸發者稽核                | `MetroSyncService`、`StationFareSyncStateHolder`、`SyncStatusVO`                 | API 回應新增欄位（向後相容）        |
| 3    | 取消機制                  | 上述全部 + `MetroSyncController`、`SyncStatusEnum`、新增 2 個例外                | **前端需處理新的 `CANCELLED` 狀態** |

對應的 commit message：

```
fix(metro): 修正票價同步中斷後誤判為成功
feat(metro): 票價同步記錄觸發者以供稽核
feat(metro): 新增票價同步取消機制
```

### 前端協作事項

項目 2、3 需同步通知前端：

- 輪詢回應新增 `triggeredBy` 欄位，可顯示於同步狀態面板
- 新增 `CANCELLED` 狀態，需與 `FAILED` 區分樣式（前者為中性提示，後者為錯誤）
- 取消為非同步生效，按下取消後應顯示「正在中止...」的過渡狀態，持續輪詢至狀態轉為 `CANCELLED`

---

#### 七、附錄：常見疑問

**Q：管理員登出後，同步還會繼續嗎？**

會，且這是正確行為。原因有三：

1. `doSyncAllStationFare()` 標註 `@Async`，Controller 回 202 後 HTTP 執行緒即結束，同步跑在獨立執行緒池上，與 HTTP 連線、session 無關
2. 背景執行緒全程未引用 `SecurityContext`；登出只清除 cookie 與撤銷 Redis 中的 refresh token，僅影響「之後的新請求」
3. 對 TDX 的請求使用 `TDXApiClientConfig` 自身的 client credentials，並非管理員的 token，不會因登出而失效

唯一會中斷同步的情況是應用程式關閉，或執行緒被明確 interrupt——這正是缺口一要處理的場景。

**Q：為什麼不在登出時自動取消同步？**

會使任務生命週期與 token 效期耦合，導致長時間任務在 token 自然過期時被意外終止，且無法解釋排程觸發的同步（根本沒有使用者可登出）。
