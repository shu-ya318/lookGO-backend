package com.mli.lookgo.core.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link BirthDateRangeValidator} 的邊界值測試（6 歲與 150 歲皆為含端點的閉區間）。
 *
 * @author D5042101
 * @since 2026.07.12
 */
class BirthDateRangeValidatorTest {

    private final BirthDateRangeValidator validator = new BirthDateRangeValidator();

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    @Test
    @DisplayName("null 視為通過（是否必填交由 @NotNull 決定）")
    void nullIsValid() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    @DisplayName("剛滿 6 歲當天為合法")
    void exactlySixYearsOldIsValid() {
        assertTrue(validator.isValid(today.minusYears(6), null));
    }

    @Test
    @DisplayName("未滿 6 歲（剛滿 6 歲的前一天）為非法")
    void youngerThanSixIsInvalid() {
        assertFalse(validator.isValid(today.minusYears(6).plusDays(1), null));
    }

    @Test
    @DisplayName("剛滿 150 歲當天為合法")
    void exactlyMaxAgeIsValid() {
        assertTrue(validator.isValid(today.minusYears(150), null));
    }

    @Test
    @DisplayName("超過 150 歲（剛滿 150 歲的後一天）為非法")
    void olderThanMaxAgeIsInvalid() {
        assertFalse(validator.isValid(today.minusYears(150).minusDays(1), null));
    }
}
