package com.mli.lookgo.module.user.enums;

/**
 * 使用者會員等級相關的列舉。
 *
 * @author D5042101
 * @since 2026.06.07
 */
public enum MembershipTier {

    BASIC(1, "BASIC"),
    PREMIUM(2, "PREMIUM");

    private final int id;
    private final String label;

    MembershipTier(int id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * 回傳對應的資料庫數值（membership_tier_id）。
     *
     * @return 資料庫儲存的 id
     */
    public int getId() {
        return id;
    }

    /**
     * 回傳對應的固定字串值，供 VO 序列化後回傳給前端使用。
     *
     * @return 固定字串值（如 "BASIC"、"PREMIUM"）
     */
    public String getLabel() {
        return label;
    }

    /**
     * 根據資料庫數值取得對應的 {@link MembershipTier}。
     *
     * @param id
     * @return 對應的 {@link MembershipTier}
     * @throws IllegalArgumentException
     */
    public static MembershipTier fromId(int id) {
        for (MembershipTier tier : values()) {
            if (tier.id == id) {
                return tier;
            }
        }

        throw new IllegalArgumentException("無效的 MembershipTier id: " + id);
    }
}
