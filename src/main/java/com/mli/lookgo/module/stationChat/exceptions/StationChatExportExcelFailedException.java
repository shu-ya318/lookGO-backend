package com.mli.lookgo.module.stationChat.exceptions;

/**
 * 處理匯出車站當日聊天紀錄 excel 檔失敗的自定義例外。
 *
 * @author D5042101
 * @since 2026.07.05
 */
public class StationChatExportExcelFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StationChatExportExcelFailedException(String message) {
        super(message);
    }
}
