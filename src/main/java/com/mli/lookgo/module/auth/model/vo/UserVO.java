package com.mli.lookgo.module.auth.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳顯示使用者相關資料的物件。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Schema(description = "回傳顯示使用者相關資料的物件")
public class UserVO {

    @Schema(description = "id", example = "1")
    private Long id;

    @Schema(description = "電子郵件地址", example = "user@example.com")
    private String email;

    @Schema(description = "使用者名稱", example = "測試用使用者")
    private String username;

    @Schema(description = "會員方案 id (1=FREE, 2=BASIC, 3=PREMIUM)", example = "1")
    private Long membershipTierId;

    @Schema(description = "建立時間", example = "2026-05-25T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間", example = "2026-05-25T10:00:00")
    private LocalDateTime updatedAt;

    public UserVO() {
    }

    public UserVO(Long id, String email, String username, Long membershipTierId,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.membershipTierId = membershipTierId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public Long getMembershipTierId() {
        return membershipTierId;
    }

    public void setMembershipTierId(Long membershipTierId) {
        this.membershipTierId = membershipTierId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserVO{id=" + id + ", email=" + email + ", username=" + username + "}";
    }
}
