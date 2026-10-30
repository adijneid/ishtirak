package org.example.ishtirak.server.service;

import org.example.ishtirak.common.dto.LoginRequest;
import org.example.ishtirak.common.dto.LoginResponse;
import org.example.ishtirak.server.exception.ForbiddenException;
import org.example.ishtirak.server.exception.UnauthorizedException;
import org.example.ishtirak.server.model.AppUser;
import org.example.ishtirak.server.repository.AppUserRepository;
import org.example.ishtirak.server.security.TokenService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthService(AppUserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository
                .findByUsername(request.username().trim())
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid username or password"));

        boolean validPassword = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!validPassword) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid username or password");
        }
        // checked after the password, so a disabled account is only revealed to its real owner
        if (!user.isActive()) {
            throw new ForbiddenException("ACCOUNT_DISABLED", "This account is disabled");
        }

        return new LoginResponse(
                tokenService.createToken(user.getId(), user.getUsername(), user.getRole()),
                user.getRole().name(),
                user.getFullName(),
                user.getId(),
                user.getUsername()
        );
    }
}
