package org.example.ishtirak.server.exception;

import org.example.ishtirak.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorsBecome500WithAGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("SQL password=secret leaked"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("An unexpected error occurred", response.getBody().message());
        assertFalse(response.getBody().message().contains("secret"));
    }

    @Test
    void apiExceptionsKeepTheirStatusAndCode() {
        ResponseEntity<ErrorResponse> response =
                handler.handleApi(new ConflictException("OVERPAYMENT", "too much"));
        assertEquals(409, response.getStatusCode().value());
        assertEquals("OVERPAYMENT", response.getBody().code());
    }
}
