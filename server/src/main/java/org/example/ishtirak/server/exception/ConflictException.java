package org.example.ishtirak.server.exception;

import org.springframework.http.HttpStatus;

/** 409 business-rule conflict, with the specific code from the proposal (DUPLICATE_PHONE, OVERPAYMENT, ...). */
public class ConflictException extends ApiException {
    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
