package com.ledgercore.dto.request;

import com.ledgercore.enums.TransactionCategory;
import com.ledgercore.validation.MonetaryAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Transfer funds between two accounts owned by the same user")
public record TransferRequest(
        @Schema(description = "Source account ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "Source account ID is required")
        UUID sourceAccountId,

        @Schema(description = "Destination account ID", example = "550e8400-e29b-41d4-a716-446655440001")
        @NotNull(message = "Destination account ID is required")
        UUID destinationAccountId,

        @Schema(description = "Amount to transfer", example = "250.00")
        @NotNull(message = "Amount is required")
        @MonetaryAmount
        BigDecimal amount,

        @Schema(description = "Transaction category", example = "OTHER")
        @NotNull(message = "Category is required")
        TransactionCategory category,

        @Schema(description = "Optional description", example = "Moving savings")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Schema(description = "Idempotency key to prevent duplicate processing", example = "txr-20260624-001")
        String idempotencyKey
) {
}
