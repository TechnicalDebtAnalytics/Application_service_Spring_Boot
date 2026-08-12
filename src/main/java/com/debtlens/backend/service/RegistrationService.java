package com.debtlens.backend.service;

import com.debtlens.backend.dto.request.RegistrationRequest;
import com.debtlens.backend.dto.response.RegistrationResponse;

public interface RegistrationService {

    RegistrationResponse register(
            RegistrationRequest request
    );
}