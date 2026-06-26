package com.ledgercore.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * Validator for the {@link MonetaryAmount} annotation.
 * Ensures financial amounts meet strict requirements:
 * - Not null
 * - Strictly positive (> 0)
 * - At most 1,000,000
 * - At most 2 decimal places (e.g., 99.99 is valid; 99.999 is not)
 */
public class MonetaryAmountValidator implements ConstraintValidator<MonetaryAmount, BigDecimal> {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");
    private static final int MAX_DECIMAL_PLACES = 2;

    @Override
    public void initialize(MonetaryAmount constraintAnnotation) {
        // No initialization required
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            setMessage(context, "Amount must not be null");
            return false;
        }

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            setMessage(context, "Amount must be greater than zero");
            return false;
        }

        if (value.compareTo(MAX_AMOUNT) > 0) {
            setMessage(context, "Amount must not exceed 1,000,000");
            return false;
        }

        if (value.stripTrailingZeros().scale() > MAX_DECIMAL_PLACES) {
            setMessage(context, "Amount must have at most 2 decimal places");
            return false;
        }

        return true;
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
