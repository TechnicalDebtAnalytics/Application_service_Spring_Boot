package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CompanyMemberResponseDTO(
        Long memberId,
        Long userId,
        String email,
        String githubUsername,
        String name,
        LocalDateTime joinedAt,
        List<RepositoryResponseDTO> assignedRepositories
) {}
