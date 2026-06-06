package com.mli.lookgo.module.auth.model.vo;

import java.time.LocalDate;

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

    @Schema(description = "部門 id", example = "1")
    private Long departmentId;

    @Schema(description = "建立時間(當下，格式yyyy-mm-dd)", example = "2026-05-25")
    private LocalDate createdAt;

    public UserVO() {
    }

    public UserVO(Long id, String email, String username, Long departmentId, LocalDate createdAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.departmentId = departmentId;
        this.createdAt = createdAt;
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserVO{id=" + id + ", email=" + email + ", username=" + username + "}";
    }
}
