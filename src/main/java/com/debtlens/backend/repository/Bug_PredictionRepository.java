package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Bug_Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Bug_PredictionRepository
        extends JpaRepository<Bug_Prediction, Long> {

    // Resolves the single prediction associated with one class metric record.
    Optional<Bug_Prediction>
    findByClassMetricsClassId(Long classId);

    // Supports idempotent persistence when an ML result is received.
    boolean existsByClassMetricsClassId(Long classId);

    // Retrieves all per-class bug predictions belonging to one analysis job.
    List<Bug_Prediction>
    findByClassMetricsAnalysisJobAnalysisId(Long analysisId);
}
