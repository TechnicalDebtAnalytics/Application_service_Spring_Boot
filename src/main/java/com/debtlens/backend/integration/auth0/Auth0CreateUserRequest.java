package com.debtlens.backend.integration.auth0;

public record Auth0CreateUserRequest(
        String connection,
        String email,
        String password
) {
}