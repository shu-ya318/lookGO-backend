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

    /** MyBatis @Options(keyProperty) 會在 INSERT 後將資料庫產生的 id 回填至此欄位。 */
    private Long id;

    @Schema(description = "電子郵件地址", example = "user@example.com")
    @NotBlank(message = "請輸入 Email!")
    @Email(message = "Email 格式不正確!")
    private String email;

    @Schema(description = "使用者名稱", example = "測試使用者")
    @NotBlank(message = "請輸入使用者名稱!")
    private String username;

    @Schema(description = "使用者密碼", example = "password12345")
    @NotBlank(message = "請輸入密碼!")
    @Size(min = 8, max = 20, message = "密碼長度必須為 8-20 個字元!")
    private String password;

    @Schema(description = "會員方案 id (1=FREE, 2=BASIC, 3=PREMIUM)", example = "1")
    @NotNull(message = "請輸入會員方案 id!")
    @Min(value = 1, message = "會員方案 id 必須是正整數！")
    @Digits(integer = 16, fraction = 0, message = "會員方案 id 最多到16個字元!")
    private Long membershipTierId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getMembershipTierId() {
        return membershipTierId;
    }

    public void setMembershipTierId(Long membershipTierId) {
        this.membershipTierId = membershipTierId;
    }

    @Override
    public String toString() {
        return "SignupDTO{" + "email='" + email + "', username='" + username + "', password='[PROTECTED]"
                + "', membershipTierId=" + membershipTierId + '}';
    }
}
