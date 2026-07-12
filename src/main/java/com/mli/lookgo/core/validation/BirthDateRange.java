package com.mli.lookgo.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 驗證出生日期換算年齡是否介於 6 歲（含）至 150 歲（含）之間，null 視為通過（是否必填交由 {@code @NotNull} 決定）。
 *
 * @author D5042101
 * @since 2026.07.12
 */
@Documented
@Constraint(validatedBy = BirthDateRangeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface BirthDateRange {
    String message() default "出生日期年齡必須介於 6 歲至 150 歲之間!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
