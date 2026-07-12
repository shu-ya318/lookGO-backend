package com.mli.lookgo.module.user.exceptions;

/**
 * 上傳的頭像圖片格式或大小不合法時拋出的例外。
 *
 * @author D5042101
 * @since 2026.07.12
 */
public class InvalidAvatarException extends RuntimeException {

    public InvalidAvatarException(String message) {
        super(message);
    }
}
