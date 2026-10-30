package org.example.ishtirak.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Amounts are Lebanese pounds (LBP), whole numbers. */
public record TariffRequest(
        @NotNull
        YearMonth billingMonth,
        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 0)
        BigDecimal pricePerKwh,
        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 0)
        BigDecimal fee5a,
        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 0)
        BigDecimal fee10a,
        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 0)
        BigDecimal feePerExtra5a,
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        @Digits(integer = 3, fraction = 1)
        BigDecimal ruralUpliftPct
) {
}
