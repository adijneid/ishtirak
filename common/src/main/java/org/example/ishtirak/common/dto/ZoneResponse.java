package org.example.ishtirak.common.dto;

public record ZoneResponse(
        Integer id,
        String name,
        boolean isRural,
        long subscriberCount
) {
}
