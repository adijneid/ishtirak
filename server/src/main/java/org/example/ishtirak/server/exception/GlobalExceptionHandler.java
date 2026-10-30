package org.example.ishtirak.server.exception;

import org.example.ishtirak.common.dto.ErrorResponse;
import org.example.ishtirak.common.dto.FieldErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Turns every exception into the single JSON error shape of the proposal:
 * {status, code, message, fieldErrors[], timestamp}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Database constraint name -> proposal error code, for races the service checks cannot see. */
    private static final Map<String, String> CONSTRAINT_CODES = Map.of(
            "uq_zone_name", "DUPLICATE_NAME",
            "uq_subscriber_phone", "DUPLICATE_PHONE",
            "uq_subscription_meter_serial", "DUPLICATE_METER",
            "uq_tariff_month_billing_month", "DUPLICATE_MONTH",
            "uq_invoice_reading_not_voided", "DUPLICATE_INVOICE",
            "uq_app_user_username", "DUPLICATE_USERNAME"
    );

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getFieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldErrorResponse(e.getField(), e.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", fields);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed",
                List.of(new FieldErrorResponse(ex.getParameterName(), "is required")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed",
                List.of(new FieldErrorResponse(ex.getName(), "has an invalid value: " + ex.getValue())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request body is missing or is not valid JSON for this service", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "No service at " + ex.getResourcePath(), List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        String text = String.valueOf(ex.getMostSpecificCause().getMessage());
        for (Map.Entry<String, String> entry : CONSTRAINT_CODES.entrySet()) {
            if (text.contains(entry.getKey())) {
                return build(HttpStatus.CONFLICT, entry.getValue(), "The value is already in use", List.of());
            }
        }
        log.warn("Constraint violation: {}", text);
        return build(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION",
                "The operation breaks a database rule", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // details are logged on the server, the client only gets a generic message
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                List<FieldErrorResponse> fieldErrors) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), code, message, fieldErrors, LocalDateTime.now()));
    }
}
