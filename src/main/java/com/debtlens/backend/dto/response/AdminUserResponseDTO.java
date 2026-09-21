package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;

public record AdminUserResponseDTO(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String githubUsername,
        Boolean emailVerified,
        String companyRole,
        String companyName,
        LocalDateTime createdAt
) {
}
