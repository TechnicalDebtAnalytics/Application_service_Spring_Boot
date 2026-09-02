package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;

public record AdminCompanyResponseDTO(
        Long companyId,
        String companyName,
        String githubOrganizationUrl,
        String superAdminName,
        String superAdminEmail,
        int totalRepositories,
        int totalMembers,
        LocalDateTime createdAt
) {
}