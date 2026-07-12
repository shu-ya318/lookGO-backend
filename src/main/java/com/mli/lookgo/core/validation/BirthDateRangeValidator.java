package com.mli.lookgo.core.validation;

import java.time.LocalDate;
import java.time.ZoneOffset;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link BirthDateRange} 的驗證邏輯實作，以「今日」計算年齡，最小 6 歲（含）、最大 150 歲（含）。
 *
 * @author D5042101
 * @since 2026.07.12
 */
public class BirthDateRangeValidator implements ConstraintValidator<BirthDateRange, LocalDate> {

    private static final int MIN_AGE = 6;
    private static final int MAX_AGE = 150;

    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        return !birthDate.isAfter(today.minusYears(MIN_AGE))
                && !birthDate.isBefore(today.minusYears(MAX_AGE));
    }
}
