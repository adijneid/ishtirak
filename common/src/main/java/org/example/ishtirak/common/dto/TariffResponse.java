package org.example.ishtirak.common.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record TariffResponse(
        Integer id,
        YearMonth billingMonth,
        BigDecimal pricePerKwh,
        BigDecimal fee5a,
        BigDecimal fee10a,
        BigDecimal feePerExtra5a,
        BigDecimal ruralUpliftPct,
        boolean locked
) {
}
