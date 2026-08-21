package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Analysis_Status_History;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Analysis_Status_HistoryRepository
        extends JpaRepository<Analysis_Status_History, Long> {

    // Returns one analysis job's lifecycle events in chronological order.
    List<Analysis_Status_History>
    findByAnalysisJobAnalysisIdOrderByTimestampAsc(Long analysisId);
}
