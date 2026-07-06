package com.mli.lookgo.module.stationBookmark.exceptions;

/**
 * 找不到指定車站書籤，或該書籤已被軟刪除時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.06
 */
public class BookmarkNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BookmarkNotFoundException(String message) {
        super(message);
    }
}
