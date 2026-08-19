package com.debtlens.backend.dto.response;

public record RegistrationResponse(
        String message,
        String auth0UserId,
        String email,
        String role,
        String githubUsername
) {
}
