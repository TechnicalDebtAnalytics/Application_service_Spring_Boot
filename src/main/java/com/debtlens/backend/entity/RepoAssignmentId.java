package com.debtlens.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the composite database key for a repository assignment.
 * A Member/Repository pair uniquely identifies one access assignment.
 *
 * JPA key classes implement equals and hashCode so persistence contexts,
 * collections, and entity lookups can compare composite identifiers reliably.
 */
@Embeddable
public class RepoAssignmentId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "repository_id")
    private Long repositoryId;

    public RepoAssignmentId() {
    }

    public RepoAssignmentId(Long memberId, Long repositoryId) {
        this.memberId = memberId;
        this.repositoryId = repositoryId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof RepoAssignmentId that)) {
            return false;
        }

        return Objects.equals(memberId, that.memberId)
                && Objects.equals(repositoryId, that.repositoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, repositoryId);
    }
}
