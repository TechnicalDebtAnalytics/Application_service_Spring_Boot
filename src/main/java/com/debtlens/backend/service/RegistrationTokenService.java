package com.debtlens.backend.service;

public interface RegistrationTokenService {

    String createToken(
            String auth0UserId,
            String email,
            String firstName,
            String lastName,
            String role
    );
}
