package com.debtlens.backend.dto.response;

public record CompanyAvailableRepoDTO(
        Long githubRepositoryId,
        String name,
        String fullName,
        String htmlUrl,
        String defaultBranch,
        String description,
        boolean alreadyAdded,
        String language,
        Integer stargazersCount
) {
}
