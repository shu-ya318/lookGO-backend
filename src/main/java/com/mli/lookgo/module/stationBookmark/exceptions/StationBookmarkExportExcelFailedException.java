package com.mli.lookgo.module.stationBookmark.exceptions;

/**
 * 處理匯出車站書籤 excel 檔失敗的自定義例外。
 *
 * @author D5042101
 * @since 2026.07.06
 */
public class StationBookmarkExportExcelFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StationBookmarkExportExcelFailedException(String message) {
        super(message);
    }
}
