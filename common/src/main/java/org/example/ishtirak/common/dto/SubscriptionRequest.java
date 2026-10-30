package org.example.ishtirak.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequest(
        @NotNull
        Integer subscriberId,
        @NotBlank
        @Size(max = 30)
        String meterSerial,
        @NotNull
        @Min(5)
        @Max(60)
        Integer amperage,
        @NotNull
        @DecimalMin("0.0")
        @Digits(integer = 9, fraction = 1)
        BigDecimal initialReading,
        @NotNull
        LocalDate startDate
) {
}
