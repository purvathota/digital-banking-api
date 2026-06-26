package com.ledgercore.dto.response;

import com.ledgercore.enums.TransactionCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Budget details response")
public record BudgetResponse(
        @Schema(description = "Budget ID", example = "550e8400-e29b-41d4-a716-446655440003")
        UUID id,

        @Schema(description = "Spending category", example = "GROCERIES")
        TransactionCategory category,

        @Schema(description = "Monthly spending limit", example = "500.00")
        BigDecimal monthlyLimit
) {
}
