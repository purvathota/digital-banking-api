package com.ledgercore.dto.request;

import com.ledgercore.enums.TransactionCategory;
import com.ledgercore.validation.MonetaryAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Set or update a monthly budget limit per category per account")
public record CreateBudgetRequest(
        @Schema(description = "Account ID to set budget for", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "Account ID is required")
        UUID accountId,

        @Schema(description = "Spending category", example = "GROCERIES")
        @NotNull(message = "Category is required")
        TransactionCategory category,

        @Schema(description = "Monthly spending limit", example = "500.00")
        @NotNull(message = "Monthly limit is required")
        @MonetaryAmount
        BigDecimal monthlyLimit
) {
}
