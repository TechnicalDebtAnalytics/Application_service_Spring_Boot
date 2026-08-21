package com.debtlens.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for sending repository contributor invitations.
 */
public record InvitationRequestDTO(
        @NotNull(message = "Repository ID is required")
        Long repositoryId,

        @NotEmpty(message = "At least one contributor invitation must be provided")
        @Valid
        List<ContributorInviteDTO> contributors
) {
    public record ContributorInviteDTO(
            @NotBlank(message = "GitHub username is required")
            String githubUsername,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email
    ) {}
}