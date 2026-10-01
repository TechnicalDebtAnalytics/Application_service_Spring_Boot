package com.debtlens.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record LinkGithubInstallationDTO(
        @NotNull(message = "GitHub installation ID is required")
        Long installationId
) {
}
