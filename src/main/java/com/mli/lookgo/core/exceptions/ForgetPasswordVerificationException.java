package com.mli.lookgo.core.exceptions;

/**
 * 忘記密碼驗證失敗時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.01
 */
public class ForgetPasswordVerificationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ForgetPasswordVerificationException(String message) {
        super(message);
    }
}
