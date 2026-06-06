package com.mli.lookgo.module.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 處理使用者註冊相關的資料傳輸物件。
 * 
 * @author D5042101
 * @since 2026.06.06
 */
@Schema(description = "處理使用者註冊相關的資料傳輸物件")
public class SignupDTO {

    @Schema(description = "電子郵件地址", example = "user@example.com")
    @NotBlank(message = "請輸入 Email!")
    @Email(message = "Email 格式不正確!")
    String email;

    @Schema(description = "使用者名稱", example = "測試使用者")
    @NotBlank(message = "請輸入使用者名稱!")
    String username;

    @Schema(description = "使用者密碼", example = "password12345")
    @NotBlank(message = "請輸入密碼!")
    @Size(min = 8, max = 20, message = "密碼長度必須為 8-20 個字元!")
    String password;

    @Schema(description = "部門 id", example = "1")
    @NotNull(message = "請輸入部門 id (最多到16個字元)!")
    @Min(value = 1, message = "部門 id 必須是正整數！")
    @Digits(integer = 16, fraction = 0, message = "部門 id 最多到16個字元!")
    Long departmentId;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    @Override
    public String toString() {
        return "SignupDTO{" + "email='" + email + "', username='" + username + "', password='" + password
                + "', departmentId=" + departmentId + '}';
    }
}
