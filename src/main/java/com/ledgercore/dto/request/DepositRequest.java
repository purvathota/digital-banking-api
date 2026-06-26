package com.ledgercore.dto.request;

import com.ledgercore.enums.TransactionCategory;
import com.ledgercore.validation.MonetaryAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Deposit funds into an account")
public record DepositRequest(
        @Schema(description = "Amount to deposit", example = "500.00")
        @NotNull(message = "Amount is required")
        @MonetaryAmount
        BigDecimal amount,

        @Schema(description = "Transaction category", example = "OTHER")
        @NotNull(message = "Category is required")
        TransactionCategory category,

        @Schema(description = "Optional description", example = "Salary deposit")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Schema(description = "Idempotency key to prevent duplicate processing", example = "dep-20260624-001")
        String idempotencyKey
) {
}
