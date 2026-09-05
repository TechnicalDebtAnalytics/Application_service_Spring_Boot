package com.debtlens.backend.dto.response;

public record SystemHealthItemDTO(
        String name,
        String key,
        String description,
        String status,
        String details
) {
}
