package com.mli.lookgo.module.stationBookmark.exceptions;

/**
 * 使用者建立的車站書籤數量已達會員等級上限時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.06
 */
public class BookmarkLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BookmarkLimitExceededException(String message) {
        super(message);
    }
}
