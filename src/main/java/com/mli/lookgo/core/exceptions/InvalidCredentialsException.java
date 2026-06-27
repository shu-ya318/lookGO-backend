package com.mli.lookgo.core.exceptions;

/**
 * 帳號或密碼驗證失敗時拋出的例外。
 *
 * @author D5042101
 * @since 2026.06.06
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
