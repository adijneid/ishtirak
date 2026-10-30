package org.example.ishtirak.common.dto;

import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public record BillingRunRequest(
        @NotNull
        YearMonth month
) {
}
