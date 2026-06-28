package com.mli.lookgo.module.user.exceptions;

/**
 * 嘗試變更管理員帳號狀態時拋出的例外。
 *
 * @author D5042101
 * @since 2026.06.29
 */
public class AdminStatusModificationException extends RuntimeException {

    public AdminStatusModificationException(String message) {
        super(message);
    }
}
