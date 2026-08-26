package com.debtlens.backend.service;

import com.debtlens.backend.dto.request.InvitationRequestDTO;
import com.debtlens.backend.dto.response.InvitationResponseDTO;

import java.util.List;

public interface InvitationService {

    /**
     * Creates and sends invitations to the specified contributors for a repository.
     *
     * @param request Payload containing repositoryId and list of contributors (username + email).
     * @return List of created invitation response DTOs.
     */
    List<InvitationResponseDTO> sendInvitations(InvitationRequestDTO request);

    /**
     * Retrieves all invitations for a specific repository.
     *
     * @param repositoryId Primary key of the repository.
     * @return List of invitations for the repository.
     */
    List<InvitationResponseDTO> getInvitationsByRepository(Long repositoryId);

    /**
     * Retrieves all invitations for all repositories under a specific company.
     *
     * @param companyId Primary key of the company.
     * @return List of invitations for the company.
     */
    List<InvitationResponseDTO> getInvitationsByCompany(Long companyId);

    /**
     * Retrieves all pending invitations addressed to the currently authenticated user.
     *
     * @return List of pending invitations for the current user.
     */
    List<InvitationResponseDTO> getMyPendingInvitations();

    /**
     * Accepts a pending invitation, grants access by creating Member and RepoAssignment records.
     *
     * @param invitationId Primary key of the invitation.
     * @return Updated invitation DTO with status ACCEPTED.
     */
    InvitationResponseDTO acceptInvitation(Long invitationId);

    /**
     * Rejects a pending invitation.
     *
     * @param invitationId Primary key of the invitation.
     * @return Updated invitation DTO with status REJECTED.
     */
    InvitationResponseDTO rejectInvitation(Long invitationId);
}