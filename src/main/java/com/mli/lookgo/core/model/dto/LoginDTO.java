package com.mli.lookgo.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 處理使用者登入相關的資料傳輸物件。
 * 
 * @author D5042101
 * @since 2026.06.06
 */
@Schema(description = "處理使用者登入相關的資料傳輸物件")
public class LoginDTO {

    @Schema(description = "使用者 Email", example = "admin@example.com")
    @NotBlank(message = "請輸入 Email!")
    @Email(message = "Email 格式不正確!")
    String email;

    @Schema(description = "使用者密碼", example = "admin12345")
    @NotBlank(message = "請輸入密碼!")
    @Size(min = 8, max = 20, message = "密碼長度必須為 8-20 個字元!")
    String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginDTO{" + "email='" + email + "', password='" + password + '\'' + '}';
    }
}
