package com.ledgercore.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Monthly spending summary by category")
public record MonthlySummaryResponse(
        @Schema(description = "Year and month", example = "2026-06")
        String yearMonth,

        @Schema(description = "Spending totals by category")
        Map<String, BigDecimal> summaryByCategory
) {
}
