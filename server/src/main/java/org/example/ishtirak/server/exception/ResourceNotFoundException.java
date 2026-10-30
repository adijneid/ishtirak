package org.example.ishtirak.server.exception;

import org.springframework.http.HttpStatus;

/** 404 NOT_FOUND (or a more specific code such as TARIFF_NOT_FOUND). */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public ResourceNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
