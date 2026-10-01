package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;

public record AdminActivityResponseDTO(
        String type,
        String title,
        String description,
        String targetType,
        Long targetId,
        LocalDateTime occurredAt
) {
}
