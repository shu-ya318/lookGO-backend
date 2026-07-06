package com.mli.lookgo.module.stationBookmark.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 處理刪除車站書籤相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.07.06
 */
@Schema(description = "處理刪除車站書籤相關的資料傳輸物件")
public class BookmarkIdDTO {

    @Schema(description = "書籤 id", example = "1")
    @NotNull(message = "請輸入書籤id!")
    private Integer bookmarkId;

    public Integer getBookmarkId() {
        return bookmarkId;
    }

    public void setBookmarkId(Integer bookmarkId) {
        this.bookmarkId = bookmarkId;
    }

    @Override
    public String toString() {
        return "BookmarkIdDTO{bookmarkId=" + bookmarkId + "}";
    }
}
