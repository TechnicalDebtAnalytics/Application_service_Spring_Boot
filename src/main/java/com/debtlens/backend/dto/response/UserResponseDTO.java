package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponseDTO(
        Long userId,
        String auth0UserId,
        String firstName,
        String lastName,
        String email,
        String githubUsername,
        Boolean emailVerified,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
