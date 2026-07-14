package com.mli.lookgo.module.user.model.vo;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳頭像更新結果的物件，供更新頭像與移除頭像（恢復預設頭像）共用。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳頭像更新結果的物件")
public class UpdateAvatarVO {

    @Schema(description = "頭像（base64 data URI 或預設頭像相對路徑）", example = "/assets/default-avatar.png")
    private String avatar;

    @Schema(description = "更新時間 (ISO 8601 UTC)", example = "2026-07-14T08:00:00Z")
    private ZonedDateTime updatedAt;

    public UpdateAvatarVO() {
    }

    public UpdateAvatarVO(String avatar, ZonedDateTime updatedAt) {
        this.avatar = avatar;
        this.updatedAt = updatedAt;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
