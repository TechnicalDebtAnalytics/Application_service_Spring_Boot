package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record SystemHealthResponseDTO(
        String overallStatus,
        LocalDateTime timestamp,
        List<SystemHealthItemDTO> services
) {
}
