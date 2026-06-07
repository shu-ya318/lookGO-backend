package com.mli.lookgo.module.auth.model.entity;

import java.time.LocalDate;
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
    private Integer id;

    @Schema(description = "會員等級 id", example = "1")
    private Integer membershipTierId;

    @Schema(description = "角色 id", example = "1")
    private Integer roleId;

    @Schema(description = "電子郵件地址", example = "user@example.com")
    private String email;

    @Schema(description = "使用者密碼", example = "password12345")
    private String password;

    @Schema(description = "使用者名稱", example = "測試使用者")
    private String username;

    @Schema(description = "出生日期(yyyy-MM-dd)", example = "2000-01-01")
    private LocalDate birthDate;

    @Schema(description = "狀態 (0=停用(軟刪除), 1=正常使用)", example = "1")
    private Integer status;

    @Schema(description = "建立時間(yyyy-MM-dd HH:mm:ss)", example = "2026-05-25 10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間(yyyy-MM-dd HH:mm:ss)", example = "2026-05-25 10:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "最後登入時間(yyyy-MM-dd HH:mm:ss)", example = "2026-05-25 10:00:00")
    private LocalDateTime lastLoginAt;

    public User() {
    }

    public User(Integer id, Integer membershipTierId, Integer roleId, String email, String password, String username,
            LocalDate birthDate, Integer status, LocalDateTime createdAt, LocalDateTime updatedAt,
            LocalDateTime lastLoginAt) {
        this.id = id;
        this.membershipTierId = membershipTierId;
        this.roleId = roleId;
        this.email = email;
        this.password = password;
        this.username = username;
        this.birthDate = birthDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMembershipTierId() {
        return membershipTierId;
    }

    public void setMembershipTierId(Integer membershipTierId) {
        this.membershipTierId = membershipTierId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", membershipTierId=" + membershipTierId +
                ", roleId=" + roleId +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", birthDate=" + birthDate +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lastLoginAt=" + lastLoginAt +
                '}';
    }
}
