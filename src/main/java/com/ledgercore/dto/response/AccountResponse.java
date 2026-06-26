package com.ledgercore.dto.response;

import com.ledgercore.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Account details response")
public record AccountResponse(
        @Schema(description = "Account ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Account type", example = "SAVINGS")
        AccountType accountType,

        @Schema(description = "Current balance", example = "1500.00")
        BigDecimal balance,

        @Schema(description = "Currency code", example = "USD")
        String currency,

        @Schema(description = "Account creation timestamp", example = "2026-06-24T13:00:00")
        LocalDateTime createdAt
) {
}
