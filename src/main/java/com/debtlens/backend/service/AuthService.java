package com.debtlens.backend.service;

import com.debtlens.backend.config.Auth0RoleConfig;
import com.debtlens.backend.dto.request.RegisterRequest;
import com.debtlens.backend.dto.response.RegistrationResponse;
import com.debtlens.backend.integration.auth0.Auth0Client;
import com.debtlens.backend.integration.auth0.Auth0UserResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final Auth0Client auth0Client;
    private final Auth0RoleConfig auth0RoleConfig;

    public AuthService(
            Auth0Client auth0Client,
            Auth0RoleConfig auth0RoleConfig
    ) {
        this.auth0Client = auth0Client;
        this.auth0RoleConfig = auth0RoleConfig;
    }

    public RegistrationResponse register(RegisterRequest request) {

        String roleId = auth0RoleConfig.getSystemUser();

        if (roleId == null || roleId.isBlank()) {
            throw new IllegalStateException(
                    "SYSTEM_USER Auth0 role ID is not configured"
            );
        }

        String managementToken =
                auth0Client.getManagementApiToken();

        Auth0UserResponse auth0User =
                auth0Client.createUser(
                        managementToken,
                        request.email(),
                        request.password()
                );

        auth0Client.assignRole(
                managementToken,
                auth0User.userId(),
                roleId
        );

        return new RegistrationResponse(
                "User registered successfully",
                auth0User.userId(),
                auth0User.email(),
                "SYSTEM_USER"
        );
    }
}