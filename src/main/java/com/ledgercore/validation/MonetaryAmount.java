package com.ledgercore.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a BigDecimal monetary amount is:
 * <ul>
 *   <li>Not null</li>
 *   <li>Greater than zero</li>
 *   <li>Not greater than 1,000,000</li>
 *   <li>Has at most 2 decimal places</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = MonetaryAmountValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MonetaryAmount {

    String message() default "Amount must be positive, at most 1,000,000, and have at most 2 decimal places";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
