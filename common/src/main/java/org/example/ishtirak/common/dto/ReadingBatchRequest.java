package org.example.ishtirak.common.dto;

import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;
import java.util.List;

public record ReadingBatchRequest(
        @NotNull
        YearMonth month,
        @NotNull
        List<ReadingEntry> entries
) {
}
