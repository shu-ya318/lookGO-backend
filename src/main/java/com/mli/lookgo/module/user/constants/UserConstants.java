package com.mli.lookgo.module.user.constants;

/**
 * 使用者模組共用的常數。
 *
 * @author D5042101
 * @since 2026.07.12
 */
public final class UserConstants {

    /**
     * 預設頭像圖檔的相對路徑（與資料庫 {@code DF_users_avatar} 預設值一致）。
     * 移除頭像時將 avatar 更新回此值。
     */
    public static final String DEFAULT_AVATAR_URL = "/assets/default-avatar.png";

    private UserConstants() {
    }
}
