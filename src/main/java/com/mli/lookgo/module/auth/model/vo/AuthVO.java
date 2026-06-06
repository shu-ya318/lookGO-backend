package com.mli.lookgo.module.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳顯示身分驗證相關資料的物件。
 * 
 * @author D5042101
 * @since 2026.06.06
 */
@Schema(description = "回傳顯示身分驗證相關資料的物件")
public class AuthVO {

    @Schema(description = "存取憑證", example = "")
    String accessToken;

    @Schema(description = "刷新憑證", example = "")
    String refreshToken;

    public AuthVO() {
    }

    public AuthVO(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
