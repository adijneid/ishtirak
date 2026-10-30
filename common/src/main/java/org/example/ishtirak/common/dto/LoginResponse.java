package org.example.ishtirak.common.dto;

public record LoginResponse(
        String token,
        String role,
        String fullName,
        Integer userId,
        String username
) {
}
