package org.example.ishtirak.common.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SubscriptionEndRequest(
        @NotNull
        LocalDate endDate
) {
}
