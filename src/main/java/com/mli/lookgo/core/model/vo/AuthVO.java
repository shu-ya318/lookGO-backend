package com.mli.lookgo.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳顯示身分驗證相關資料的物件。
 * 
 * @author D5042101
 * @since 2026.06.06
 */
@Schema(description = "回傳顯示身分驗證相關資料的物件")
public class AuthVO {

    @Schema(description = "存取 token", example = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJmNGU2NjJmZC1mZDEzLTQ2MjgtOTk3Ni1lZTc0ZGY3ZGNhMGEiLCJzdWIiOiJjMjQ4ODYwYS05ZGMxLTQ0MGMtYjIxOC0zYTZiNjMyN2RhYWUiLCJpZCI6MSwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTc4MDgzODk4MiwiZXhwIjoxNzgwODM5ODgyfQ.4E2N7KxE2O9t9kizcYT77zXY99Y3o8z7_aW8_x9kXNk")
    private String accessToken;

    @Schema(description = "刷新 token", example = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJlZjZkYWRiMC0wYjdmLTQ4OWYtOTIwOC0yOTg4MmRiNGY3NjIiLCJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzgwODM4OTgyLCJleHAiOjE3ODE0NDM3ODJ9.uBF2AFlNZK8XymVk0DyYbTKQHkVF6BhbK-s1_kl1PgE")
    private String refreshToken;

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
