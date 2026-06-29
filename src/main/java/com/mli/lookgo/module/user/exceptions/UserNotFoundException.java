package com.mli.lookgo.module.user.exceptions;

/**
 * 找不到指定使用者時拋出的例外。
 *
 * @author D5042101
 * @since 2026.06.06
 */
public class UserNotFoundException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

    public UserNotFoundException(String message) {
        super(message);
    }
}
