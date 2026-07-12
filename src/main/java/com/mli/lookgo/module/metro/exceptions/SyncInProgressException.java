package com.mli.lookgo.module.metro.exceptions;

/**
 * 票價同步已在進行中，重複觸發時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.12
 */
public class SyncInProgressException extends RuntimeException {
    public SyncInProgressException(String message) {
        super(message);
    }
}
