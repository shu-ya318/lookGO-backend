package com.mli.lookgo.module.stationChat.exceptions;

/**
 * 使用者當日發送留言則數已達會員等級上限時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.04
 */
public class ChatDailyLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChatDailyLimitExceededException(String message) {
        super(message);
    }
}
