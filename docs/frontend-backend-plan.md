我要調整或新增多個功能，包含修改現有功能模組的程式碼，以及為新功能建立前端頁面元件、後端 API與邏輯。
請扮演高階架構顧問，幫我一次性生成一份全盤實作規劃的 Markdown 檔，除了已知要求，請完整納入漏掉考量的邏輯或UIUX設計，更新目前分支的 `docs/frontend-backend-plan.md`。
功能如下
一、生日值驗證

1. 後端
   (1) 檔案: core 模組和user模組定義或調用birthDate欄位的.java檔
   (2) 功能: 驗證最大值到150歲(包含)，最小值在6歲上(包含)，並在驗證失敗有提示。
2. 前端
   (1) 檔案: 調用birthDate的.jsx檔。
   (2) 功能: 驗證最大值到150歲(包含)，最小值在6歲上(包含)，並在驗證失敗有提示。

二、個人頭像修改

1. 後端
   (1) 檔案: src\main\resources\schema.sql 的 users 表和 user 模組相關檔案。
   (2) 功能:
   - users表加入avatar欄位，允許null欄位。預設值為預設頭像的url
   - 新增更新頭像API，可上傳base 64 圖檔或移除頭像恢復預設值。必須驗證圖像檔案格式與大小(上限5MB)。
     (3) 額外在計畫中提供修改 tables 結構的 SQL 語句。
2. 前端
   (1) 檔案:
   - 使用Avatar元件的src\components\header 和 src\components\stationChat\MessageBubble.tsx。
   - 接收並儲存資料的src\services\user和src\stores\userStore.ts
     (2) 功能: - 移除目前Avatar元件，登入後改成讀取並顯示userInfo的avatar欄位，並將圖片放大到合理的尺寸。- 提供按鈕可編輯頭像，上傳新圖檔或移除頭像恢復預設值，並有提示文字說明頭像的檔案大小上限。

三、下拉選單車站搜尋優化

1. 前端
   (1) 檔案:src\pages\HomePage.tsx 、src\components\StationAutocomplete.tsx。
   (2) 功能:
   - StationAutocomplete 的 stationCode 移除原本用()包住的jsx element呈現，改參考src\components\metroMap\StationInfoCard.tsx用<chip>元件呈現 stationCode。
   - HomePage 調用的StationAutocomplete，改為下拉選中後自動執行handleSearch，移除原本提交搜尋按鈕。

四、匯出車站書籤檔名與匯出旅程規劃檔名加上 username 方便區辨。

1. 前端
   (1) 檔案: stationBookmark 和 tripPlan 的對應頁面或子元件.jsx檔案。
   (2) 功能: 加入讀取的username做為.xlsx檔名的一部分

五、車站書籤頁和旅程規劃顯示所有資料時，加上接受排序

1. 後端
   (1) 檔案: stationBookmark 和 tripPlan 模組對應的.java檔案。
   (2) 功能: 接收前端傳入的排序條件，包含依照更新時間從近到遠或從遠到近，並在資料庫查詢時加入排序條件。
2. 前端
   (1) 檔案: stationBookmark 和 tripPlan 的對應頁面或子元件.jsx檔案。
   (2) 功能: 在搜尋欄元件旁加入讓使用者選擇排序方式的下拉選單，並將排序條件傳送給後端。

六、旅程規劃頁的旅程規劃名稱禁止與自身重複。提供更明確的錯誤提示。

1. 後端
   (1) 檔案: tripPlan 模組對應的.java檔和core的src\main\java\com\mli\lookgo\core\exceptions\GlobalExceptionHandler.java。
   (2) 功能:createTripPlan或 updateTripPlanName時，業務邏輯在呼叫dao前驗證禁止同 user 儲存同名旅程的驗證，違反則拋出tripPlanNameDuplicationException，並在前端顯示錯誤訊息。
2. 前端
   (1) 檔案: 所有.jsx檔案。
   (2) 功能: src\components\tripPlan\TripPlanEditorDialog.tsx和src\components\tripPlan\UpdateTripPlanNameDialog.tsx在欄位名稱下方多出一行，用MUI theme 的 info顏色提示不能和既有的旅程規劃重複名稱。

七、車站聊天室頁的發送訊息功能。

1. 後端
   (1) 檔案: stationChat 模組對應的.java檔案和core的src\main\java\com\mli\lookgo\core\exceptions\GlobalExceptionHandler.java。
   (2) 功能: 判斷目前寫法是否能禁止 base 64 圖片等非文字訊息的傳入，如果不行請在業務邏輯加入驗證，違反則拋出自定義例外，返回給前端顯示錯誤訊息。
2. 前端
   (1) 檔案: src\pages\StationChatPage.tsx。
   (2) 功能: 輸入元件的 placeholder 改成請輸入內容，只接受文字訊息的提示

八、車站管理頁：同步票價 API 請求修改

1. 後端
   (1) 檔案: metro 模組對應的.java檔案。
   (2) 功能:
   - 改 syncAllStationFare，一旦發送成功就返回202成功狀態碼，表示已開始背景同步，不讓 HTTP 連線卡住。
   - 另外新增 更新同步票價狀態的 API，返回status狀態資訊，供前端透過短輪詢判斷目前狀態。
   - 在適合的API 的成功返回訊息提供目前請求進度的百分比數值。
2. 前端
   (1) 檔案:src\components\admin\MetroSyncSection.tsx 和 src\services\metroSync 對應檔案。
   (2) 功能:
   - src\components\admin\MetroSyncSection.tsx
     - 同步按鈕第一次請求成功時，先顯示第一支API請求成功訊息。
     - 接著以 http prolling 方式，每30秒重新請求更新狀態的API。依照返回狀態，若仍正在請求則顯示同步中...。
     - 請求過程因斷線或登出等非預期因素中斷，發起中斷請求的訊號。
