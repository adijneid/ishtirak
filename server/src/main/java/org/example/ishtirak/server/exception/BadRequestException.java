package org.example.ishtirak.server.exception;

import org.example.ishtirak.common.dto.FieldErrorResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

/** 400 VALIDATION_ERROR for rules Bean Validation cannot express (e.g. amperage multiple of 5). */
public class BadRequestException extends ApiException {
    public BadRequestException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed",
                List.of(new FieldErrorResponse(field, message)));
    }
}
