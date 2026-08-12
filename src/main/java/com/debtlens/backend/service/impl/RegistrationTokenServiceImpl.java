package com.debtlens.backend.service.impl;

import com.debtlens.backend.service.RegistrationTokenService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegistrationTokenServiceImpl
        implements RegistrationTokenService {

    @Override
    public String createToken(
            String auth0UserId,
            String email,
            String firstName,
            String lastName,
            String role
    ) {
        return UUID.randomUUID().toString();
    }
}