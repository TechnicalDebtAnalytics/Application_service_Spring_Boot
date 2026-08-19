package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CompanyResponseDTO(
        Long companyId,
        String companyName,
        String githubOrganizationUrl,
        String githubOrganizationName,
        Long createdByUserId,
        String createdByName,
        int totalRepositories,
        List<RepositoryResponseDTO> repositories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
