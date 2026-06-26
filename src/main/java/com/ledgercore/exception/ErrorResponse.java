package com.ledgercore.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standardized error response body returned by all error handling in the API.
 * All timestamps are in ISO-8601 format.
 */
@Schema(description = "Standard error response")
public record ErrorResponse(
        @Schema(description = "Machine-readable error code", example = "VALIDATION_ERROR")
        String error,

        @Schema(description = "Human-readable error message", example = "Amount must be greater than zero")
        String message,

        @Schema(description = "ISO-8601 timestamp of when the error occurred", example = "2026-06-24T13:00:00Z")
        String timestamp
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, Instant.now().toString());
    }
}
