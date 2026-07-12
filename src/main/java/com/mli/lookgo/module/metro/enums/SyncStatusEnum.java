package com.mli.lookgo.module.metro.enums;

/**
 * 票價背景同步作業的狀態列舉。
 * 序列化時以常數名稱（如 "RUNNING"）輸出，供前端輪詢比對。
 *
 * @author D5042101
 * @since 2026.07.12
 */
public enum SyncStatusEnum {

    IDLE("IDLE", "尚未執行"),
    RUNNING("RUNNING", "同步進行中"),
    SUCCESS("SUCCESS", "同步成功"),
    FAILED("FAILED", "同步失敗");

    private final String code;
    private final String chineseName;

    SyncStatusEnum(String code, String chineseName) {
        this.code = code;
        this.chineseName = chineseName;
    }

    public String getCode() {
        return code;
    }

    public String getChineseName() {
        return chineseName;
    }
}
