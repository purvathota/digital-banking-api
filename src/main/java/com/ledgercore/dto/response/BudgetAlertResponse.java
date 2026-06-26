package com.ledgercore.dto.response;

import com.ledgercore.enums.TransactionCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Budget threshold alert")
public record BudgetAlertResponse(
        @Schema(description = "Spending category", example = "GROCERIES")
        TransactionCategory category,

        @Schema(description = "Monthly budget limit", example = "500.00")
        BigDecimal budgetLimit,

        @Schema(description = "Amount spent in current month", example = "420.00")
        BigDecimal currentSpend,

        @Schema(description = "Percentage of budget used", example = "84.0")
        BigDecimal percentageUsed,

        @Schema(description = "Human-readable alert message",
                example = "Warning: You have used 84.0% of your GROCERIES budget (420.00 / 500.00)")
        String message
) {
}
