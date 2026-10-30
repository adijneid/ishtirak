package org.example.ishtirak.common.dto;

import java.math.BigDecimal;

/** One row of a batch. Rows are validated one by one on the server (see ReadingService). */
public record ReadingEntry(
        Integer subscriptionId,
        BigDecimal value
) {
}
