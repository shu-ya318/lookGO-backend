package com.mli.lookgo.core.result;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳密碼更新結果的物件，密碼本身不對外回傳，僅回傳異動時間。
 * 供 {@code UserService.updatePassword}、{@code AuthService.resetPassword} 共用。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳密碼更新結果的物件")
public class UpdatePasswordVO {

    @Schema(description = "更新時間 (ISO 8601 UTC)", example = "2026-07-14T08:00:00Z")
    private ZonedDateTime updatedAt;

    public UpdatePasswordVO() {
    }

    public UpdatePasswordVO(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
