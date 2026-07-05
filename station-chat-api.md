# Station Chat 車站聊天室 API 文件

本文件說明位於 [StationChatController.java](src/main/java/com/mli/lookgo/module/stationChat/controller/StationChatController.java) 所定義的車站聊天室與公告相關的 HTTP 接口。

---

## 1. 依車站 id 分頁取得車站聊天留言

* **Endpoint**: `POST /api/v1/station-chat/get-message-by-station-id`
* **說明**: 取得指定車站的歷史聊天留言，支援分頁，依建立時間新到舊排序。
* **Request Parameters** (Query / Form 參數):
  * `stationId` (Integer, 必填): 車站 id。
  * `page` (int, 選填, 預設為 `0`): 頁碼 (從 0 起算)。
  * `size` (int, 選填, 預設為 `16`): 每頁筆數。
* **Request Body**: 無
* **Response**:
  * **200 OK** (成功取得車站聊天留言):
    ```json
    {
      "content": [
        {
          "id": 1,
          "username": "小明",
          "chatType": 1,
          "content": "這裡的電梯正在維修",
          "tripPlanId": 10,
          "fromStationName": "淡水站",
          "toStationName": "台北車站",
          "fareType": 1,
          "farePrice": 45.00,
          "transferCount": 1,
          "createdAt": "2026-07-03T12:00:00Z"
        }
      ],
      "page": 0,
      "size": 16,
      "totalElements": 1,
      "totalPages": 1
    }
    ```
  * **401 Unauthorized** (Token 無效或已過期):
    ```json
    "未授權錯誤，token無效或已過期"
    ```
  * **404 Not Found** (找不到指定車站):
    ```json
    "找不到 id:1 的車站!"
    ```

---

## 2. 依車站 id 取得車站聊天公告

* **Endpoint**: `POST /api/v1/station-chat/get-announcement-by-station-id`
* **說明**: 取得指定車站的所有公告，依建立時間新到舊排序。
* **Request Body** (application/json):
  ```json
  {
    "stationId": 1
  }
  ```
* **Response**:
  * **200 OK** (成功取得車站聊天公告):
    ```json
    [
      {
        "id": 1,
        "stationId": 1,
        "content": "本站電梯本週維修暫停使用",
        "createdByUsername": "admin",
        "createdAt": "2026-07-04T12:00:00Z",
        "updatedAt": "2026-07-04T12:00:00Z"
      }
    ]
    ```
  * **401 Unauthorized** (Token 無效或已過期):
    ```json
    "未授權錯誤，token無效或已過期"
    ```
  * **404 Not Found** (找不到指定車站):
    ```json
    "找不到 id:1 的車站!"
    ```

---

## 3. 新增車站聊天公告

* **Endpoint**: `POST /api/v1/station-chat/create-announcement`
* **說明**: 於指定車站新增一筆公告。需具有 `ADMIN` 角色權限。
* **Request Body** (application/json):
  ```json
  {
    "stationId": 1,
    "content": "本站電梯本週維修暫停使用"
  }
  ```
* **Response**:
  * **200 OK** (公告新增成功):
    ```json
    {
      "message": "公告新增成功"
    }
    ```
  * **401 Unauthorized** (Token 無效或已過期):
    ```json
    "未授權錯誤，token無效或已過期"
    ```
  * **403 Forbidden** (權限不足):
    ```json
    "權限不足，無法操作!"
    ```
  * **404 Not Found** (找不到當前使用者或指定車站):
    ```json
    "找不到 id:1 的車站!"
    ```

---

## 4. 編輯車站聊天公告

* **Endpoint**: `POST /api/v1/station-chat/update-announcement`
* **說明**: 更新指定公告 id 的內容。需具有 `ADMIN` 角色權限。
* **Request Body** (application/json):
  ```json
  {
    "announcementId": 1,
    "content": "本站電梯已恢復正常使用"
  }
  ```
* **Response**:
  * **200 OK** (公告編輯成功):
    ```json
    {
      "message": "公告編輯成功"
    }
    ```
  * **401 Unauthorized** (Token 無效或已過期):
    ```json
    "未授權錯誤，token無效或已過期"
    ```
  * **403 Forbidden** (權限不足):
    ```json
    "權限不足，無法操作!"
    ```
  * **404 Not Found** (找不到指定公告):
    ```json
    "找不到 id:1 的公告!"
    ```

---

## 5. 刪除車站聊天公告

* **Endpoint**: `POST /api/v1/station-chat/delete-announcement`
* **說明**: 軟刪除指定公告 id 的公告。需具有 `ADMIN` 角色權限。
* **Request Body** (application/json):
  ```json
  {
    "announcementId": 1
  }
  ```
* **Response**:
  * **200 OK** (公告刪除成功):
    ```json
    {
      "message": "公告刪除成功"
    }
    ```
  * **401 Unauthorized** (Token 無效或已過期):
    ```json
    "未授權錯誤，token無效或已過期"
    ```
  * **403 Forbidden** (權限不足):
    ```json
    "權限不足，無法操作!"
    ```
  * **404 Not Found** (找不到指定公告):
    ```json
    "找不到 id:1 的公告!"
    ```
