package com.mli.lookgo.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳忘記密碼驗證成功後的重設密碼 token。
 *
 * @author D5042101
 * @since 2026.06.30
 */
@Schema(description = "回傳忘記密碼驗證成功後的重設密碼 token")
public class ForgetPasswordVO {

    @Schema(description = "重設密碼 token（效期 15 分鐘），以此 token 呼叫重設密碼 API", example = "h150Z1e2FUHP_0DUxm46h4rojyntzS90awLNFUqE0q0")
    private String resetPasswordToken;

    public ForgetPasswordVO() {
    }

    public ForgetPasswordVO(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }
}
