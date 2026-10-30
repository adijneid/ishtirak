package org.example.ishtirak.common.dto;

/** A rejected row or a non-blocking warning in a reading batch. */
public record ReadingIssue(
        Integer subscriptionId,
        String code,
        String message
) {
}
