package com.mli.lookgo.module.user.model.vo;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.mli.lookgo.module.user.enums.MembershipTier;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用於回傳出生日期更新結果的物件。
 *
 * @author D5042101
 * @since 2026.07.14
 */
@Schema(description = "回傳出生日期更新結果的物件")
public class UpdateBirthDateVO {

    @Schema(description = "出生日期(yyyy-MM-dd)", example = "2000-01-01")
    private LocalDate birthDate;

    @Schema(description = "會員方案，僅於本次異動觸發自動升級時才有值 (BASIC, PREMIUM)", example = "PREMIUM", nullable = true)
    private MembershipTier membershipTier;

    @Schema(description = "更新時間 (ISO 8601 UTC)", example = "2026-07-14T08:00:00Z")
    private ZonedDateTime updatedAt;

    public UpdateBirthDateVO() {
    }

    public UpdateBirthDateVO(LocalDate birthDate, MembershipTier membershipTier, ZonedDateTime updatedAt) {
        this.birthDate = birthDate;
        this.membershipTier = membershipTier;
        this.updatedAt = updatedAt;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public MembershipTier getMembershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(MembershipTier membershipTier) {
        this.membershipTier = membershipTier;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
