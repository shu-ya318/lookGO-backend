package com.mli.lookgo.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 處理使用者重設密碼相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.13
 */
@Schema(description = "處理使用者重設密碼相關的資料傳輸物件")
public class ResetPasswordDTO {

    @Schema(description = "使用者重設的新密碼", example = "password6789")
    @NotBlank(message = "請輸入重設的新密碼!")
    @Size(min = 8, max = 20, message = "新密碼長度必須為 8-20 個字元!")
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "ResetPasswordDTO{}";
    }
}
