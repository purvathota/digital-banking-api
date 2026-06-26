package com.ledgercore.dto.request;

import com.ledgercore.enums.TransactionCategory;
import com.ledgercore.validation.MonetaryAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Withdraw funds from an account")
public record WithdrawRequest(
        @Schema(description = "Amount to withdraw", example = "100.00")
        @NotNull(message = "Amount is required")
        @MonetaryAmount
        BigDecimal amount,

        @Schema(description = "Transaction category", example = "GROCERIES")
        @NotNull(message = "Category is required")
        TransactionCategory category,

        @Schema(description = "Optional description", example = "ATM withdrawal")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
