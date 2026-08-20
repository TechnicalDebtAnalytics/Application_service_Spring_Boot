package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Analysis_Job;
import com.debtlens.backend.entity.AnalysisJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Analysis_JobRepository
        extends JpaRepository<Analysis_Job, Long> {

    // Returns a repository's analysis history, newest request first.
    List<Analysis_Job>
    findByRepositoryRepositoryIdOrderByStartedAtDesc(Long repositoryId);

    // Returns analyses requested by a user, newest request first.
    List<Analysis_Job>
    findByStartedByUserIdOrderByStartedAtDesc(Long userId);

    // Supports lifecycle queues and operational status views.
    List<Analysis_Job>
    findByStatusOrderByStartedAtAsc(AnalysisJobStatus status);
}
