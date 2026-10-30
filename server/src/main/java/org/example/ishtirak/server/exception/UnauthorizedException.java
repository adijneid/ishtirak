package org.example.ishtirak.server.exception;

import org.springframework.http.HttpStatus;

/** 401: wrong credentials, missing or invalid token. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
