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

    List<Invitation> findByRepositoryRepositoryIdAndStatus(Long repositoryId, InvitationStatus status);

    List<Invitation> findByRepositoryCompanyCompanyId(Long companyId);

    List<Invitation> findBySuperAdminSuperAdminId(Long superAdminId);

    // Supports status-scoped views such as an admin's pending invitations.
    List<Invitation> findBySuperAdminSuperAdminIdAndStatus(
            Long superAdminId,
            InvitationStatus status
    );

    List<Invitation> findByEmail(String email);

    // Used by the service layer to prevent duplicate pending
    // invitations for the same email and repository.
    boolean existsByEmailAndRepositoryRepositoryIdAndStatus(
            String email,
            Long repositoryId,
            InvitationStatus status
    );

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Invitation i WHERE i.status = com.debtlens.backend.entity.InvitationStatus.PENDING AND ((i.githubUsername IS NOT NULL AND LOWER(i.githubUsername) = LOWER(:githubUsername)) OR (i.email IS NOT NULL AND LOWER(i.email) = LOWER(:email)))")
    List<Invitation> findPendingForUser(
            @org.springframework.data.repository.query.Param("githubUsername") String githubUsername,
            @org.springframework.data.repository.query.Param("email") String email
    );
}
