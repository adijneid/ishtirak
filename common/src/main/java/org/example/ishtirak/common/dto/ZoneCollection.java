package org.example.ishtirak.common.dto;

import java.math.BigDecimal;

public record ZoneCollection(
        Integer zoneId,
        String zone,
        BigDecimal billed,
        BigDecimal collected
) {
}
