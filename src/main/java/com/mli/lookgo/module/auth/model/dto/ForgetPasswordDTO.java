package com.mli.lookgo.module.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 處理使用者忘記密碼相關的資料傳輸物件。
 * 
 * @author D5042101
 * @since 2026.06.13
 */
@Schema(description = "處理使用者忘記密碼相關的資料傳輸物件")
public class ForgetPasswordDTO {

    @Schema(description = "使用者 Email", example = "user@example.com")
    @NotBlank(message = "請輸入 Email!")
    @Email(message = "Email 格式不正確!")
    String email;
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    @Override
    public String toString() {
        return "LoginDTO{" + "email='" + email + '}';
    }
}
