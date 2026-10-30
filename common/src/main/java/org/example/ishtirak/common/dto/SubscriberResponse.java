package org.example.ishtirak.common.dto;

import org.example.ishtirak.common.model.SubscriberStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriberResponse(
        Integer id,
        String fullName,
        String phone,
        String address,
        Integer zoneId,
        String zoneName,
        SubscriberStatus status,
        LocalDateTime createdAt,
        BigDecimal balance
) {
}
