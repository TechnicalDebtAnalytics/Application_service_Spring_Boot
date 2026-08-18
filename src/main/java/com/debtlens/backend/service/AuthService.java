package com.debtlens.backend.service;

import com.debtlens.backend.dto.request.RegisterRequest;
import com.debtlens.backend.dto.response.RegistrationResponse;

public interface AuthService {

    RegistrationResponse register(RegisterRequest request);
}