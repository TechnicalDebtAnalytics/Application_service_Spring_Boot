package com.debtlens.backend.integration.github.dto;

public record GithubMemberValidationResponse(
        String organization,
        String username,
        boolean isMember,
        String message
) {
}
