package com.mli.lookgo.module.stationChat.exceptions;

import org.springframework.messaging.MessagingException;

/**
 * 找不到指定站點聊天留言時拋出的例外。
 *
 * @author
 * @since 2026.07.03
 */
public class StompAuthException extends MessagingException {

    private static final long serialVersionUID = 1L;

    public StompAuthException(String message) {
        super(message);
    }

    public StompAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
