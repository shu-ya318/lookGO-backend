package com.mli.lookgo.module.stationChat.exceptions;

/**
 * 車站聊天留言內容不符合「僅接受文字訊息」政策時拋出的例外（如夾帶圖片、base64 編碼或 HTML 標籤）。
 *
 * @author D5042101
 * @since 2026.07.12
 */
public class InvalidChatContentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidChatContentException(String message) {
        super(message);
    }
}
