package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Invitation;
import com.debtlens.backend.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository
        extends JpaRepository<Invitation, Long> {

    // Used when resolving an invitation link/token.
    Optional<Invitation> findByToken(String token);

    // Lists invitation history for a repository or issuing admin assignment.
    List<Invitation> findByRepositoryRepositoryId(Long repositoryId);

    List<Invitation> findBySuperAdminSuperAdminId(Long superAdminId);

    // Supports status-scoped views such as an admin's pending invitations.
    List<Invitation> findBySuperAdminSuperAdminIdAndStatus(
            Long superAdminId,
            InvitationStatus status
    );

    List<Invitation> findByEmail(String email);

    // Used by the future service layer to prevent duplicate pending
    // invitations for the same email and repository.
    boolean existsByEmailAndRepositoryRepositoryIdAndStatus(
            String email,
            Long repositoryId,
            InvitationStatus status
    );
}
