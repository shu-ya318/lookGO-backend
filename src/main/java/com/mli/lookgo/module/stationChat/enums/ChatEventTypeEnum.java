package com.mli.lookgo.module.stationChat.enums;

/**
 * 車站聊天即時事件類型相關的列舉。
 *
 * @author D5042101
 * @since 2026.07.04
 */
public enum ChatEventTypeEnum {

    NEW(1, "新訊息"),
    DELETE(2, "刪除訊息");

    private final int code;
    private final String chineseName;

    ChatEventTypeEnum(int code, String chineseName) {
        this.code = code;
        this.chineseName = chineseName;
    }

    /**
     * 回傳對應的事件代碼，供
     * {@link com.mli.lookgo.module.stationChat.model.vo.StationChatEventVO} 序列化使用。
     *
     * @return 事件代碼
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
}
