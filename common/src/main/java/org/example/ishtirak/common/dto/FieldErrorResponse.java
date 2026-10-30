package org.example.ishtirak.common.dto;

public record FieldErrorResponse(
        String field,
        String message
) {
}
