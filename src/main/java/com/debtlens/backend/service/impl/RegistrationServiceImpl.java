package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.request.RegistrationRequest;
import com.debtlens.backend.dto.response.RegistrationResponse;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.service.Auth0ManagementService;
import com.debtlens.backend.service.RegistrationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final Auth0ManagementService auth0ManagementService;
    private final UserRepository userRepository;

    public RegistrationServiceImpl(
            Auth0ManagementService auth0ManagementService,
            UserRepository userRepository
    ) {
        this.auth0ManagementService = auth0ManagementService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public RegistrationResponse register(
            RegistrationRequest request
    ) {

        // 1. Check whether email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                    "A user with this email already exists"
            );
        }

        // 2. Create user in Auth0
        String auth0UserId =
                auth0ManagementService.createUser(
                        request.getEmail(),
                        request.getPassword(),
                        request.getFirstName(),
                        request.getLastName()
                );

        // Make sure Auth0 returned a valid ID
        if (auth0UserId == null || auth0UserId.isBlank()) {
            throw new RuntimeException(
                    "Auth0 user creation failed: no Auth0 user ID returned"
            );
        }

        // 3. Assign the default role in Auth0
        auth0ManagementService.assignRole(auth0UserId);

        // 4. Create our local database user
        User user = new User();

        user.setAuth0UserId(auth0UserId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setEmailVerified(false);

        // 5. Save user
        User savedUser = userRepository.save(user);

        // 6. Return response
        return new RegistrationResponse(
                "Registration successful",
                savedUser.getAuth0UserId(),
                savedUser.getEmail(),
                request.getRole()
        );
    }
}
