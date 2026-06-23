package com.mli.lookgo.module.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/**
 * 用於搜尋和篩選使用者資料的傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.23
 */
@Schema(description = "搜尋和篩選使用者資料的傳輸物件")
public class UserFilterRequestDTO {

    @Schema(description = "搜尋關鍵字", example = "user")
    @Pattern(regexp = "^(?!\\s*$).+", message = "請輸入搜尋關鍵字!")
    private String keyword;

    public UserFilterRequestDTO() {
    }

    public UserFilterRequestDTO(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
