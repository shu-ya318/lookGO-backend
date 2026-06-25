package com.mli.lookgo.module.user.model.vo;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.mli.lookgo.module.user.enums.MembershipTier;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.enums.UserStatus;

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

    @Schema(description = "電子郵件地址", example = "admin@example.com")
    private String email;

    @Schema(description = "使用者名稱", example = "測試用使用者")
    private String username;

    @Schema(description = "會員方案 (FREE, BASIC, PREMIUM)", example = "FREE")
    private MembershipTier membershipTier;

    @Schema(description = "角色 (USER, ADMIN)", example = "USER")
    private UserRole role;

    @Schema(description = "出生日期(yyyy-MM-dd)", example = "2000-01-01")
    private LocalDate birthDate;

    @Schema(description = "狀態 (DISABLED, ACTIVE)", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "建立時間 (ISO 8601 UTC)", example = "2026-06-07T21:29:20Z")
    private ZonedDateTime createdAt;

    @Schema(description = "更新時間 (ISO 8601 UTC)", example = "2026-06-07T21:29:20Z")
    private ZonedDateTime updatedAt;

    @Schema(description = "最後登入時間 (ISO 8601 UTC)", example = "2026-06-07T21:29:20Z")
    private ZonedDateTime lastLoginAt;

    public UserVO() {
    }

    public UserVO(Integer id, String email, String username, MembershipTier membershipTier, UserRole role,
            LocalDate birthDate, UserStatus status, ZonedDateTime createdAt, ZonedDateTime updatedAt,
            ZonedDateTime lastLoginAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.membershipTier = membershipTier;
        this.role = role;
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

    public MembershipTier getMembershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(MembershipTier membershipTier) {
        this.membershipTier = membershipTier;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ZonedDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(ZonedDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    @Override
    public String toString() {
        return "UserVO{id=" + id + ", email=" + email + ", username=" + username + "}";
    }
}
