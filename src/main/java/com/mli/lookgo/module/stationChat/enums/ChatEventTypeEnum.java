package com.mli.lookgo.module.stationChat.enums;

/**
 * 車站聊天即時事件類型相關的列舉，僅作為 STOMP 廣播 payload 的事件辨識標記，不寫入資料庫，
 * 交由 Jackson 以列舉常數名稱（NEW／DELETE）序列化。
 *
 * @author D5042101
 * @since 2026.07.04
 */
public enum ChatEventTypeEnum {

    NEW,
    DELETE
}
