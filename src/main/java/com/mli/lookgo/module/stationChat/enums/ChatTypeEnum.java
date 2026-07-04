package com.mli.lookgo.module.stationChat.enums;

/**
 * 站點聊天留言類型相關的列舉。
 *
 * @author D5042101
 * @since 2026.07.03
 */
public enum ChatTypeEnum {

    TEXT(1, "文字訊息"),
    TRIP_PLAN(2, "旅程分享");

    private final int code;
    private final String chineseName;

    ChatTypeEnum(int code, String chineseName) {
        this.code = code;
        this.chineseName = chineseName;
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
     * 回傳對應的中文名稱。
     *
     * @return 中文名稱
     */
    public String getChineseName() {
        return chineseName;
    }

    /**
     * 根據資料庫數值取得對應的 {@link ChatTypeEnum}。
     *
     * @param code
     * @return 對應的 {@link ChatTypeEnum}
     * @throws IllegalArgumentException
     */
    public static ChatTypeEnum fromCode(int code) {
        for (ChatTypeEnum chatType : values()) {
            if (chatType.code == code) {
                return chatType;
            }
        }

        throw new IllegalArgumentException("無效的 ChatTypeEnum code: " + code);
    }
}
