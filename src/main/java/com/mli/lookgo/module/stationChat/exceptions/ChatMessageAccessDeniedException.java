package com.mli.lookgo.module.stationChat.exceptions;

/**
 * 使用者嘗試刪除非本人留言，且非 ADMIN 角色時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.04
 */
public class ChatMessageAccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ChatMessageAccessDeniedException(String message) {
        super(message);
    }
}
