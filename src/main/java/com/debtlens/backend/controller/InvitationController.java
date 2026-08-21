package com.debtlens.backend.controller;

import com.debtlens.backend.dto.request.InvitationRequestDTO;
import com.debtlens.backend.dto.response.InvitationResponseDTO;
import com.debtlens.backend.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /**
     * Sends invitations to one or more repository contributors.
     */
    @PostMapping
    public ResponseEntity<List<InvitationResponseDTO>> sendInvitations(
            @Valid @RequestBody InvitationRequestDTO request
    ) {
        List<InvitationResponseDTO> result = invitationService.sendInvitations(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Retrieves all invitations issued for a specific repository.
     */
    @GetMapping("/repository/{repositoryId}")
    public ResponseEntity<List<InvitationResponseDTO>> getInvitationsByRepository(
            @PathVariable Long repositoryId
    ) {
        List<InvitationResponseDTO> invitations = invitationService.getInvitationsByRepository(repositoryId);
        return ResponseEntity.ok(invitations);
    }

    /**
     * Retrieves all invitations issued across a specific company.
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<InvitationResponseDTO>> getInvitationsByCompany(
            @PathVariable Long companyId
    ) {
        List<InvitationResponseDTO> invitations = invitationService.getInvitationsByCompany(companyId);
        return ResponseEntity.ok(invitations);
    }

    /**
     * Retrieves all pending invitations addressed to the currently authenticated user.
     */
    @GetMapping("/my-pending")
    public ResponseEntity<List<InvitationResponseDTO>> getMyPendingInvitations() {
        List<InvitationResponseDTO> pending = invitationService.getMyPendingInvitations();
        return ResponseEntity.ok(pending);
    }

    /**
     * Accepts a pending invitation.
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<InvitationResponseDTO> acceptInvitation(
            @PathVariable Long id
    ) {
        InvitationResponseDTO accepted = invitationService.acceptInvitation(id);
        return ResponseEntity.ok(accepted);
    }

    /**
     * Rejects a pending invitation.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<InvitationResponseDTO> rejectInvitation(
            @PathVariable Long id
    ) {
        InvitationResponseDTO rejected = invitationService.rejectInvitation(id);
        return ResponseEntity.ok(rejected);
    }
}