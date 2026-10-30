package org.example.ishtirak.client;

import org.example.ishtirak.common.dto.ErrorResponse;
import org.example.ishtirak.common.dto.FieldErrorResponse;

import java.util.List;

/** A failed service call: either the server's JSON error, or no connection at all (status 0). */
public class ApiException extends RuntimeException {
    private final int status;
    private final ErrorResponse error;

    public ApiException(ErrorResponse error) {
        super(error.message());
        this.status = error.status();
        this.error = error;
    }

    private ApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
        this.error = null;
    }

    public static ApiException connectionLost(Throwable cause) {
        return new ApiException("The server cannot be reached", cause);
    }

    public boolean isConnectionLost() {
        return status == 0;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return error == null ? "CONNECTION_LOST" : error.code();
    }

    public List<FieldErrorResponse> getFieldErrors() {
        return error == null || error.fieldErrors() == null ? List.of() : error.fieldErrors();
    }
}
