package org.example.ishtirak.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
        Integer id,
        Integer subscriberId,
        String meterSerial,
        int amperage,
        BigDecimal initialReading,
        LocalDate startDate,
        LocalDate endDate,
        boolean active
) {
}
