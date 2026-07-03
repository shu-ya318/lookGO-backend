package com.mli.lookgo.module.stationChat.exceptions;

/**
 * 找不到指定站點聊天留言時拋出的例外。
 *
 * @author 
 * @since 2026.07.03
 */
public class StationChatNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StationChatNotFoundException(String message) {
        super(message);
    }
}
