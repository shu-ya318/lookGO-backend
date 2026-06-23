package com.mli.lookgo.module.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 處理更新使用者狀態相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.23
 */
@Schema(description = "處理更新使用者狀態相關的資料傳輸物件")
public class UpdateUserStatusDTO {

    @Schema(description = "使用者 ID", example = "1")
    @NotNull(message = "使用者 ID 不能為空!")
    private Integer userId;

    @Schema(description = "使用者狀態 (0=停用, 1=正常)", example = "0")
    @NotNull(message = "使用者狀態不能為空!")
    private Integer status;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "UpdateUserStatusDTO{userId=" + userId + ", status=" + status + "}";
    }
}
