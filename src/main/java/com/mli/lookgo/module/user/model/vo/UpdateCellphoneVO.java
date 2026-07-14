package com.mli.lookgo.module.user.model.vo;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳電話號碼更新結果的物件。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳電話號碼更新結果的物件")
public class UpdateCellphoneVO {

    @Schema(description = "臺灣電話號碼（0開頭，9～10碼）", example = "0912345678")
    private String cellphone;

    @Schema(description = "更新時間 (ISO 8601 UTC)", example = "2026-07-14T08:00:00Z")
    private ZonedDateTime updatedAt;

    public UpdateCellphoneVO() {
    }

    public UpdateCellphoneVO(String cellphone, ZonedDateTime updatedAt) {
        this.cellphone = cellphone;
        this.updatedAt = updatedAt;
    }

    public String getCellphone() {
        return cellphone;
    }

    public void setCellphone(String cellphone) {
        this.cellphone = cellphone;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
