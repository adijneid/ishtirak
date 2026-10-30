package org.example.ishtirak.common.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record DashboardResponse(
        YearMonth month,
        BigDecimal billed,
        BigDecimal collected,
        BigDecimal outstanding,
        BigDecimal collectionRate,
        long missingReadings,
        List<ZoneCollection> zones
) {
}
