package org.example.ishtirak.common.dto;

import java.time.LocalDateTime;
import java.util.List;

/** The single JSON error shape returned by the server for every error. */
public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldErrorResponse> fieldErrors,
        LocalDateTime timestamp
) {
}
