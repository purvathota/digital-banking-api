package com.ledgercore.dto.request;

import com.ledgercore.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Create a new bank account")
public record CreateAccountRequest(
        @Schema(description = "Account type", example = "SAVINGS")
        @NotNull(message = "Account type is required")
        AccountType accountType
) {
}
