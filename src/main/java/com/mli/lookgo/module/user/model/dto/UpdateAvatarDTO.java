package com.mli.lookgo.module.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

/**
 * 處理更新使用者頭像相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.12
 */
@Schema(description = "處理更新使用者頭像相關的資料傳輸物件")
public class UpdateAvatarDTO {

    @Schema(description = "頭像 base64 data URI，僅支援 PNG、JPEG、WEBP 格式，解碼後大小上限 1MB", example = "data:image/png;base64,iVBORw0KGgo...")
    @NotBlank(message = "請上傳頭像圖片!")
    private String avatar;

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /**
     * 避免 log 夾帶 base64 全文，截斷為前 30 字元＋總長度。
     */
    @Override
    public String toString() {
        String preview = avatar == null ? null
                : avatar.length() <= 30 ? avatar
                        : avatar.substring(0, 30) + "...(length=" + avatar.length() + ")";

        return "UpdateAvatarDTO{avatar=" + preview + "}";
    }
}
