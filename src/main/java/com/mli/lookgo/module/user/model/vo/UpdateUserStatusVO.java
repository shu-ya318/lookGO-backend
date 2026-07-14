package com.mli.lookgo.module.user.model.vo;

import java.time.ZonedDateTime;

import com.mli.lookgo.module.user.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳使用者帳號狀態更新結果的物件。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳使用者帳號狀態更新結果的物件")
public class UpdateUserStatusVO {

    @Schema(description = "使用者 id", example = "1")
    private Integer userId;

    @Schema(description = "狀態 (DISABLED, ACTIVE)", example = "DISABLED")
    private UserStatus status;

    @Schema(description = "更新時間 (ISO 8601 UTC)", example = "2026-07-14T08:00:00Z")
    private ZonedDateTime updatedAt;

    public UpdateUserStatusVO() {
    }

    public UpdateUserStatusVO(Integer userId, UserStatus status, ZonedDateTime updatedAt) {
        this.userId = userId;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
