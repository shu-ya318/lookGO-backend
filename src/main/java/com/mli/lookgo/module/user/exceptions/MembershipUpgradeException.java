package com.mli.lookgo.module.user.exceptions;

/**
 * 不符合會員等級升級條件時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.08
 */
public class MembershipUpgradeException extends RuntimeException {

    public MembershipUpgradeException(String message) {
        super(message);
    }
}
