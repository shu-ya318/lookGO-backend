package com.mli.lookgo.module.auth.exceptions;

/**
 * 找不到指定使用者時拋出的例外。
 *
 * @author D5042101
 * @since 2026.06.06
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
