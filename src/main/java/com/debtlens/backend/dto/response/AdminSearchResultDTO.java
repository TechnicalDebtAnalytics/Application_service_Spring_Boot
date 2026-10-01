package com.debtlens.backend.dto.response;

public record AdminSearchResultDTO(
        String type,
        Long id,
        Long parentId,
        String label,
        String description
) {
}
