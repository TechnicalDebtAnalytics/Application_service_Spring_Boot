package com.debtlens.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SelectedRepoDTO(
        @NotNull(message = "GitHub repository ID is required")
        Long githubRepositoryId,

        @NotBlank(message = "Repository name is required")
        String repositoryName,

        @NotBlank(message = "Repository URL is required")
        String repositoryUrl,

        String defaultBranch
) {
}
