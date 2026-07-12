package com.mli.lookgo.module.user.model.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.mli.lookgo.core.validation.BirthDateRange;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

/**
 * 處理更新出生日期相關的資料傳輸物件。
 *
 * @author D5042101
 * @since 2026.06.21
 */
@Schema(description = "處理更新出生日期相關的資料傳輸物件")
public class UpdateBirthDateDTO {

    @Schema(description = "出生日期(yyyy-MM-dd)，換算年齡須介於 6 歲（含）至 150 歲（含）之間", example = "2000-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "請輸入出生日期!")
    @PastOrPresent(message = "出生日期不得大於今日!")
    @BirthDateRange
    private LocalDate birthDate;

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    @Override
    public String toString() {
        return "UpdateBirthDateDTO{birthDate=" + birthDate + "}";
    }
}
