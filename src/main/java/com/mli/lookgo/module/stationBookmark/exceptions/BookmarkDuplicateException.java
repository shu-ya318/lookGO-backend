package com.mli.lookgo.module.stationBookmark.exceptions;

/**
 * 使用者對同一車站重複建立書籤時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.06
 */
public class BookmarkDuplicateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BookmarkDuplicateException(String message) {
        super(message);
    }
}
