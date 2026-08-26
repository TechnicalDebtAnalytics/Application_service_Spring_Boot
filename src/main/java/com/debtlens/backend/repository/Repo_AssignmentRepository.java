package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Repo_Assignment;
import com.debtlens.backend.entity.RepoAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Repo_AssignmentRepository
        extends JpaRepository<Repo_Assignment, RepoAssignmentId> {

    // Lists every repository assigned to a Member.
    @org.springframework.data.jpa.repository.Query("SELECT ra FROM Repo_Assignment ra WHERE ra.member.memberId = :memberId")
    List<Repo_Assignment> findByMemberMemberId(@org.springframework.data.repository.query.Param("memberId") Long memberId);

    // Lists every Member assigned to a Repository.
    @org.springframework.data.jpa.repository.Query("SELECT ra FROM Repo_Assignment ra WHERE ra.repository.repositoryId = :repositoryId")
    List<Repo_Assignment> findByRepositoryRepositoryId(@org.springframework.data.repository.query.Param("repositoryId") Long repositoryId);

    // Resolves one specific Member/Repository assignment.
    @org.springframework.data.jpa.repository.Query("SELECT ra FROM Repo_Assignment ra WHERE ra.member.memberId = :memberId AND ra.repository.repositoryId = :repositoryId")
    Optional<Repo_Assignment> findByMemberMemberIdAndRepositoryRepositoryId(
            @org.springframework.data.repository.query.Param("memberId") Long memberId,
            @org.springframework.data.repository.query.Param("repositoryId") Long repositoryId
    );

    // Allows checking for an existing assignment efficiently.
    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(ra) > 0 THEN TRUE ELSE FALSE END FROM Repo_Assignment ra WHERE ra.member.memberId = :memberId AND ra.repository.repositoryId = :repositoryId")
    boolean existsByMemberMemberIdAndRepositoryRepositoryId(
            @org.springframework.data.repository.query.Param("memberId") Long memberId,
            @org.springframework.data.repository.query.Param("repositoryId") Long repositoryId
    );
}
