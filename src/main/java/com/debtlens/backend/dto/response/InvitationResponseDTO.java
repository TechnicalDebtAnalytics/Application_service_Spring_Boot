package com.debtlens.backend.dto.response;

import com.debtlens.backend.entity.InvitationStatus;

import java.time.LocalDateTime;

/**
 * Response payload representing an issued or recorded invitation.
 */
public record InvitationResponseDTO(
        Long invitationId,
        String email,
        String githubUsername,
        Long repositoryId,
        String repositoryName,
        Long companyId,
        String companyName,
        InvitationStatus status,
        String token,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {}
