package com.mli.lookgo.module.auth.model.entity;

import java.time.LocalDate;

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

    @Schema(description = "電子郵件地址", example = "user@example.com")
    private String email;

    @Schema(description = "使用者名稱", example = "測試用使用者")
    private String username;

    @Schema(description = "使用者密碼", example = "password12345")
    private String password;

    @Schema(description = "部門 id", example = "1")
    private Long departmentId;

    @Schema(description = "建立時間(當下，格式yyyy-mm-dd)", example = "2026-05-25")
    private LocalDate createdAt;

    public User() {
    }

    public User(Long id, String email, String username, String password, Long departmentId, LocalDate createdAt) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
        return "User{email=" + email + "}";
    }
}
