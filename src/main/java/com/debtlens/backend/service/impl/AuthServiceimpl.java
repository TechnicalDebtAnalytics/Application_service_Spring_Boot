package com.debtlens.backend.service.impl;

import com.debtlens.backend.config.Auth0RoleConfig;
import com.debtlens.backend.dto.request.RegisterRequest;
import com.debtlens.backend.dto.response.RegistrationResponse;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.integration.auth0.Auth0Client;
import com.debtlens.backend.integration.auth0.Auth0CreateUserResponse;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final Auth0Client auth0Client;
    private final Auth0RoleConfig auth0RoleConfig;
    private final UserRepository userRepository;

    public AuthServiceImpl(
            Auth0Client auth0Client,
            Auth0RoleConfig auth0RoleConfig,
            UserRepository userRepository
    ) {
        this.auth0Client = auth0Client;
        this.auth0RoleConfig = auth0RoleConfig;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {

        // 1. Check whether email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("A user with this email already exists");
        }

        // 2. Check whether githubUsername already exists
        if (userRepository.existsByGithubUsername(request.githubUsername().trim())) {
            throw new RuntimeException("A user with this GitHub username already exists");
        }

        // 3. Get SYSTEM_USER role ID
        String roleId = auth0RoleConfig.getSystemUser();
        if (roleId == null || roleId.isBlank()) {
            throw new IllegalStateException("SYSTEM_USER Auth0 role ID is not configured");
        }

        // 4. Get Auth0 Management API token
        String managementToken = auth0Client.getManagementApiToken();

        // 5. Create user in Auth0 (Auth0 handles authentication, not githubUsername)
        Auth0CreateUserResponse auth0User = auth0Client.createUser(
                managementToken,
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
        );

        if (auth0User == null || auth0User.userId() == null || auth0User.userId().isBlank()) {
            throw new RuntimeException("Auth0 user creation failed: no Auth0 user ID returned");
        }

        // 6. Assign SYSTEM_USER role in Auth0
        auth0Client.assignRole(managementToken, auth0User.userId(), roleId);

        // 7. Create local database user and store githubUsername locally
        User user = new User();
        user.setAuth0UserId(auth0User.userId());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setGithubUsername(request.githubUsername());
        user.setEmailVerified(false);

        // 8. Save local user to PostgreSQL DB
        User savedUser = userRepository.save(user);

        // 9. Return response with local githubUsername
        return new RegistrationResponse(
                "Registration successful",
                savedUser.getAuth0UserId(),
                savedUser.getEmail(),
                "SYSTEM_USER",
                savedUser.getGithubUsername()
        );
    }
}