package com.mli.lookgo.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 處理使用者忘記密碼相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.13
 */
@Schema(description = "處理使用者忘記密碼相關的資料傳輸物件")
public class ForgetPasswordDTO {

    @Schema(description = "使用者 Email", example = "admin@example.com")
    @NotBlank(message = "請輸入 Email!")
    @Email(message = "Email 格式不正確!")
    private String email;

    @Schema(description = "臺灣電話號碼（0開頭，9～10碼）", example = "0912345678")
    @NotBlank(message = "請輸入電話號碼!")
    @Pattern(regexp = "^0\\d{8,9}$", message = "電話號碼格式不正確，須為0開頭之9～10碼數字!")
    private String cellphone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCellphone() {
        return cellphone;
    }

    public void setCellphone(String cellphone) {
        this.cellphone = cellphone;
    }

    @Override
    public String toString() {
        return "ForgetPasswordDTO{email='" + email + "', cellphone='" + cellphone + "'}";
    }
}
