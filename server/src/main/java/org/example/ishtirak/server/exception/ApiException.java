package org.example.ishtirak.server.exception;

import lombok.Getter;
import org.example.ishtirak.common.dto.FieldErrorResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

/** Base of every expected error: carries the HTTP status and the proposal's error code. */
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<FieldErrorResponse> fieldErrors;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public ApiException(HttpStatus status, String code, String message, List<FieldErrorResponse> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors;
    }
}
