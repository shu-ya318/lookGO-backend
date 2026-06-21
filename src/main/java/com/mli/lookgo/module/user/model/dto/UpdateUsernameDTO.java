package com.mli.lookgo.module.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

/**
 * 處理更新使用者名稱相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.21
 */
@Schema(description = "處理更新使用者名稱相關的資料傳輸物件")
public class UpdateUsernameDTO {

    @Schema(description = "新的使用者名稱", example = "新名稱")
    @NotBlank(message = "請輸入使用者名稱!")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "UpdateUsernameDTO{username='" + username + "'}";
    }
}
