package com.mli.lookgo.module.user.enums;

/**
 * 使用者角色相關的列舉。
 *
 * @author D5042101
 * @since 2026.06.07
 */
public enum UserRole {

    USER(1, "USER"),
    ADMIN(2, "ADMIN");

    private final int id;
    private final String label;

    UserRole(int id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * 回傳對應的資料庫數值（role_id）。
     *
     * @return 資料庫儲存的 id
     */
    public int getId() {
        return id;
    }

    /**
     * 回傳對應的固定字串值，供 VO 序列化後回傳給前端使用。
     *
     * @return 固定字串值（如 "USER"、"ADMIN"）
     */
    public String getLabel() {
        return label;
    }

    /**
     * 根據資料庫數值取得對應的 {@link UserRole}。
     *
     * @param id 資料庫儲存的 role_id
     * @return 對應的 {@link UserRole}
     * @throws IllegalArgumentException
     */
    public static UserRole fromId(int id) {
        for (UserRole role : values()) {
            if (role.id == id) {
                return role;
            }
        }

        throw new IllegalArgumentException("無效的 UserRole id: " + id);
    }
}
