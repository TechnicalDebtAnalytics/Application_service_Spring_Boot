package com.debtlens.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents a Member's access assignment to a Repository.
 *
 * The Member/Repository pair is the identity of the assignment, so the entity
 * uses the same two columns as its composite primary key.
 */
@Entity
@Table(name = "repo_assignments")
public class Repo_Assignment {

    // EmbeddedId maps the two-column primary key as one Java value object.
    @EmbeddedId
    private RepoAssignmentId id = new RepoAssignmentId();

    // MapsId connects this relationship to the memberId part of the embedded key.
    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // MapsId connects this relationship to the repositoryId part of the embedded key.
    @MapsId("repositoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    // The service layer must verify that member.company and repository.company
    // refer to the same company before saving an assignment.
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Initializes the assignment timestamp before its first database insert.
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public RepoAssignmentId getId() {
        return id;
    }

    public void setId(RepoAssignmentId id) {
        this.id = id;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
        if (member != null) {
            if (this.id == null) {
                this.id = new RepoAssignmentId();
            }
            this.id.setMemberId(member.getMemberId());
        }
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
        if (repository != null) {
            if (this.id == null) {
                this.id = new RepoAssignmentId();
            }
            this.id.setRepositoryId(repository.getRepositoryId());
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
