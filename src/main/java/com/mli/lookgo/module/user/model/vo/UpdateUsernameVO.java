package com.mli.lookgo.module.user.model.vo;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳使用者名稱更新結果的物件。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳使用者名稱更新結果的物件")
public class UpdateUsernameVO {

    @Schema(description = "使用者名稱", example = "測試用使用者")
    private String username;

    @Schema(description = "更新時間 (ISO 8601 UTC)", example = "2026-07-14T08:00:00Z")
    private ZonedDateTime updatedAt;

    public UpdateUsernameVO() {
    }

    public UpdateUsernameVO(String username, ZonedDateTime updatedAt) {
        this.username = username;
        this.updatedAt = updatedAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
