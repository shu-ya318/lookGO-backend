package com.mli.lookgo.common.result;

/**
 * 處理 API 錯誤回應的自定義格式。
 * 
 * @author D5042101
 * @since 2026.06.06
 */
public class ApiResult {

    private String message;

    /**
     * 建立 API 錯誤回應相關的實例。
     * 
     * @param message
     */
    public ApiResult(String message) {
        this.message = message;
    }

    /**
     * 提供 API 錯誤回應相關的 getter 方法，讓外部能取得屬性。
     * 
     * @return message
     */
    public String getMessage() {
        return message;
    }

    public void setgetMessage(String message) {
        this.message = message;
    }
}
