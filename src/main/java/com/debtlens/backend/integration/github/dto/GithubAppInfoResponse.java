package com.debtlens.backend.integration.github.dto;

public record GithubAppInfoResponse(
        boolean configured,
        String appSlug,
        String installUrl
) {
}
