package com.mli.lookgo.module.stationChat.exceptions;

/**
 * 找不到指定車站聊天公告或留言時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.04
 */
public class StationChatNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StationChatNotFoundException(String message) {
        super(message);
    }
}
