package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;

public record RepositoryResponseDTO(
        Long repositoryId,
        String githubRepositoryId,
        String repositoryName,
        String repositoryUrl,
        String defaultBranch,
        LocalDateTime createdAt
) {
}
