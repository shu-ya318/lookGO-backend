package com.mli.lookgo.module.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 處理更新使用者密碼相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.21
 */
@Schema(description = "處理更新使用者密碼相關的資料傳輸物件")
public class UpdatePasswordDTO {

    @Schema(description = "舊密碼", example = "oldPassword123")
    @NotBlank(message = "請輸入舊密碼!")
    private String oldPassword;

    @Schema(description = "新密碼", example = "newPassword123")
    @NotBlank(message = "請輸入新密碼!")
    @Size(min = 8, max = 20, message = "密碼長度必須為 8-20 個字元!")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "UpdatePasswordDTO{}";
    }
}
