package com.mli.lookgo.module.auth.model.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * 處理使用者註冊相關的資料傳輸物件。
 * 
 * @author D5042101
 * @since 2026.06.06
 */
@Schema(description = "處理使用者註冊相關的資料傳輸物件")
public class SignupDTO {

    @Schema(description = "Email", example = "admin@example.com")
    @NotBlank(message = "請輸入 Email!")
    @Email(message = "Email 格式不正確!")
    private String email;

    @Schema(description = "使用者密碼", example = "admin12345")
    @NotBlank(message = "請輸入密碼!")
    @Size(min = 8, max = 20, message = "密碼長度必須為 8-20 個字元!")
    private String password;

    @Schema(description = "使用者名稱", example = "測試使用者")
    @NotBlank(message = "請輸入使用者名稱!")
    private String username;

    @Schema(description = "出生日期(yyyy-MM-dd)", example = "2000-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @PastOrPresent(message = "出生日期不得大於今日!")
    private LocalDate birthDate;

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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    @Override
    public String toString() {
        return "SignupDTO{ " + "email=" + email + ", username=" + username + " , birthDate=" + birthDate + " }";
    }
}
