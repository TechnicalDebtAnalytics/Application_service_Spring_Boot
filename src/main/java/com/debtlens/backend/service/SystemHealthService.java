package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.SystemHealthResponseDTO;

public interface SystemHealthService {

    SystemHealthResponseDTO getSystemHealth();
}
