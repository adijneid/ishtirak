package org.example.ishtirak.common.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record BillingRunResponse(
        YearMonth month,
        int created,
        int skipped,
        List<MissingReading> missingReadings,
        BigDecimal totalBilled
) {
}
