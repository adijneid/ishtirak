package org.example.ishtirak.common.dto;

import java.math.BigDecimal;

public record ReadingSheetRow(
        Integer subscriptionId,
        Integer subscriberId,
        String subscriber,
        String meterSerial,
        int amperage,
        BigDecimal previousReading,
        BigDecimal currentReading,
        boolean invoiced
) {
}
