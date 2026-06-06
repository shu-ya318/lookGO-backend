package com.mli.lookgo.module.auth.exceptions;

/**
 * 使用者重複建立時拋出的例外。
 *
 * @author D5042101
 * @since 2026.06.06
 */
public class UserDuplicateException extends RuntimeException {

    public UserDuplicateException(String message) {
        super(message);
    }
}
