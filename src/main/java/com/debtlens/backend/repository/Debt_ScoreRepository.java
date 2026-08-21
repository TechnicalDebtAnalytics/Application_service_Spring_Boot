package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Debt_Score;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Debt_ScoreRepository
        extends JpaRepository<Debt_Score, Long> {

    // Resolves the single current debt assessment for one class metric record.
    Optional<Debt_Score>
    findByClassMetricsClassId(Long classId);

    // Supports idempotent persistence when an assessment is calculated.
    boolean existsByClassMetricsClassId(Long classId);

    // Retrieves all class-level debt assessments belonging to one analysis job.
    List<Debt_Score>
    findByClassMetricsAnalysisJobAnalysisId(Long analysisId);
}
