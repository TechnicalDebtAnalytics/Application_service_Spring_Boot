package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponseDTO(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String githubUsername,
        Boolean emailVerified,
        List<AdminUserAffiliationDTO> affiliations,
        LocalDateTime createdAt
) {
}
