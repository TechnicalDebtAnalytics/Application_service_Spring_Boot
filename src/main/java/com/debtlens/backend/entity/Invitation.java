package com.debtlens.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents an invitation for an email address to receive access to a repository.
 */
@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;

    @Column(name = "email", nullable = false)
    private String email;

    // Repository that the invited user will eventually receive access to.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    // The admin assignment that issued this invitation. Storing Super_Admin
    // preserves both the issuing user and their company-level authority.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "super_admin_id", nullable = false)
    private Super_Admin superAdmin;

    // The service layer must verify that repository.company and
    // superAdmin.company refer to the same company before saving.

    // A unique opaque token will identify this invitation when its link is resolved.
    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    // String persistence keeps database values readable and avoids fragile enum ordinals.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InvitationStatus status = InvitationStatus.PENDING;

    // Every invitation must have an explicit expiration chosen by the future service.
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Ensures new entities have a lifecycle state and creation time before insertion.
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = InvitationStatus.PENDING;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Super_Admin getSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(Super_Admin superAdmin) {
        this.superAdmin = superAdmin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
