package com.mli.lookgo.module.auth.enums;

/**
 * 使用者帳號狀態相關的列舉。
 *
 * @author D5042101
 * @since 2026.06.07
 */
public enum UserStatus {

    DISABLED(0, "DISABLED"),
    ACTIVE(1, "ACTIVE");

    private final int code;
    private final String label;

    UserStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 回傳對應的資料庫數值。
     *
     * @return 資料庫儲存的數值
     */
    public int getCode() {
        return code;
    }

    /**
     * 回傳對應的固定字串值，供 VO 序列化後回傳給前端使用。
     *
     * @return 固定字串值（如 "ACTIVE"、"DISABLED"）
     */
    public String getLabel() {
        return label;
    }

    /**
     * 根據資料庫數值取得對應的 {@link UserStatus}。
     *
     * @param code
     * @return 對應的 {@link UserStatus}
     * @throws IllegalArgumentException
     */
    public static UserStatus fromCode(int code) {
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        throw new IllegalArgumentException("無效的 UserStatus code: " + code);
    }
}
