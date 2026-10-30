package org.example.ishtirak.server.security;

import org.example.ishtirak.server.exception.UnauthorizedException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Gives services access to the user that the AuthInterceptor authenticated. */
public final class CurrentUser {
    static final String ATTRIBUTE = "ishtirak.sessionUser";

    private CurrentUser() {
    }

    public static SessionUser get() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object user = attributes == null ? null
                : attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (user == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Not logged in");
        }
        return (SessionUser) user;
    }
}
