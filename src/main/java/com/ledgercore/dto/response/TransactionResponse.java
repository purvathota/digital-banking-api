package com.ledgercore.dto.response;

import com.ledgercore.enums.TransactionCategory;
import com.ledgercore.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Transaction details response")
public record TransactionResponse(
        @Schema(description = "Transaction ID", example = "550e8400-e29b-41d4-a716-446655440002")
        UUID id,

        @Schema(description = "Transaction type", example = "DEPOSIT")
        TransactionType type,

        @Schema(description = "Transaction category", example = "OTHER")
        TransactionCategory category,

        @Schema(description = "Transaction amount", example = "500.00")
        BigDecimal amount,

        @Schema(description = "Description of the transaction", example = "Salary deposit")
        String description,

        @Schema(description = "Transaction timestamp", example = "2026-06-24T13:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Budget alerts triggered by this transaction")
        List<BudgetAlertResponse> alerts
) {
    /**
     * Convenience constructor for transaction responses without alerts.
     */
    public TransactionResponse(UUID id, TransactionType type, TransactionCategory category,
                               BigDecimal amount, String description, LocalDateTime createdAt) {
        this(id, type, category, amount, description, createdAt, List.of());
    }
}
