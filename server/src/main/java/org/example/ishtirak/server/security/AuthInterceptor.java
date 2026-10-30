package org.example.ishtirak.server.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.ishtirak.common.model.Role;
import org.example.ishtirak.server.exception.ForbiddenException;
import org.example.ishtirak.server.exception.UnauthorizedException;
import org.example.ishtirak.server.model.AppUser;
import org.example.ishtirak.server.repository.AppUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs before every /api/** call except login:
 * missing/invalid token -> 401, disabled account -> 403 ACCOUNT_DISABLED,
 * COLLECTOR calling an @AdminOnly service -> 403 FORBIDDEN.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final TokenService tokenService;
    private final AppUserRepository userRepository;

    public AuthInterceptor(TokenService tokenService, AppUserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedException("UNAUTHORIZED", "Missing token, please log in");
        }
        SessionUser tokenUser = tokenService.verify(header.substring("Bearer ".length()).trim());

        // re-read the user so that disabling an account or changing a role takes effect immediately
        AppUser user = userRepository.findById(tokenUser.id())
                .orElseThrow(() -> new UnauthorizedException("INVALID_TOKEN", "Unknown user, please log in again"));
        if (!user.isActive()) {
            throw new ForbiddenException("ACCOUNT_DISABLED", "This account is disabled");
        }
        SessionUser sessionUser = new SessionUser(user.getId(), user.getUsername(), user.getRole());

        if (handler instanceof HandlerMethod method && sessionUser.role() != Role.ADMIN) {
            boolean adminOnly = method.hasMethodAnnotation(AdminOnly.class)
                    || method.getBeanType().isAnnotationPresent(AdminOnly.class);
            if (adminOnly) {
                throw new ForbiddenException("FORBIDDEN", "This service is reserved for the administrator");
            }
        }
        request.setAttribute(CurrentUser.ATTRIBUTE, sessionUser);
        return true;
    }
}
