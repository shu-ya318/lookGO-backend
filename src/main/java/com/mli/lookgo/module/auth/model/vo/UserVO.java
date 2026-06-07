package com.mli.lookgo.module.auth.model.vo;

import java.time.LocalDate;
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
    private Integer id;

    @Schema(description = "電子郵件地址", example = "user@example.com")
    private String email;

    @Schema(description = "使用者名稱", example = "測試用使用者")
    private String username;

    @Schema(description = "會員方案 id (1=FREE, 2=BASIC, 3=PREMIUM)", example = "1")
    private Integer membershipTierId;

    @Schema(description = "角色 id", example = "1")
    private Integer roleId;

    @Schema(description = "生日", example = "2000-01-01")
    private LocalDate birthDate;

    @Schema(description = "狀態 (0=停用(軟刪除), 1=正常使用)", example = "1")
    private Integer status;

    @Schema(description = "建立時間", example = "2026-05-25T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間", example = "2026-05-25T10:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "最後登入時間", example = "2026-05-25T10:00:00")
    private LocalDateTime lastLoginAt;

    public UserVO() {
    }

    public UserVO(Integer id, String email, String username, Integer membershipTierId, Integer roleId,
            LocalDate birthDate, Integer status, LocalDateTime createdAt, LocalDateTime updatedAt,
            LocalDateTime lastLoginAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.membershipTierId = membershipTierId;
        this.roleId = roleId;
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
        return "UserVO{id=" + id + ", email=" + email + ", username=" + username + "}";
    }
}
