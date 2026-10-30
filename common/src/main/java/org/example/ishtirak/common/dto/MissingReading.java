package org.example.ishtirak.common.dto;

public record MissingReading(
        Integer subscriptionId,
        String subscriber,
        String meterSerial,
        String zone
) {
}
