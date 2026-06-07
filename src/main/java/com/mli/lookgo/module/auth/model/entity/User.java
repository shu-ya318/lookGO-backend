package com.mli.lookgo.module.auth.model.entity;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 處理使用者相關的資料。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Schema(description = "處理使用者相關的資料")
public class User {

    @Schema(description = "id", example = "1")
    private Long id;

    @Schema(description = "會員方案 id", example = "1")
    private Long membershipTierId;

    @Schema(description = "角色 id", example = "1")
    private Long roleId;

    @Schema(description = "電子郵件地址", example = "user@example.com")
    private String email;

    @Schema(description = "使用者密碼", example = "password12345")
    private String password;

    @Schema(description = "使用者名稱", example = "測試用使用者")
    private String username;

    @Schema(description = "建立時間", example = "2026-05-25T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間", example = "2026-05-25T10:00:00")
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(Long id, Long membershipTierId, Long roleId, String email, String password, String username,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.membershipTierId = membershipTierId;
        this.roleId = roleId;
        this.email = email;
        this.password = password;
        this.username = username;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMembershipTierId() {
        return membershipTierId;
    }

    public void setMembershipTierId(Long membershipTierId) {
        this.membershipTierId = membershipTierId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
        return "User{email=" + email + "}";
    }
}
