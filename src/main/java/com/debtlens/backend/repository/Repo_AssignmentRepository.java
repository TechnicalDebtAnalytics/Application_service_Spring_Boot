package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Repo_Assignment;
import com.debtlens.backend.entity.RepoAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Repo_AssignmentRepository
        extends JpaRepository<Repo_Assignment, RepoAssignmentId> {

    // Lists every repository assigned to a Member.
    List<Repo_Assignment> findByMemberMemberId(Long memberId);

    // Lists every Member assigned to a Repository.
    List<Repo_Assignment> findByRepositoryRepositoryId(Long repositoryId);

    // Resolves one specific Member/Repository assignment.
    Optional<Repo_Assignment>
    findByMemberMemberIdAndRepositoryRepositoryId(
            Long memberId,
            Long repositoryId
    );

    // Allows the future service to check for an existing assignment efficiently.
    boolean existsByMemberMemberIdAndRepositoryRepositoryId(
            Long memberId,
            Long repositoryId
    );
}
