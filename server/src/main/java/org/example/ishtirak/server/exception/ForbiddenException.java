package org.example.ishtirak.server.exception;

import org.springframework.http.HttpStatus;

/** 403: disabled account or a role that may not call the service. */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
