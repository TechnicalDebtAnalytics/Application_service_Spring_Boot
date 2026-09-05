package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Analysis_Status_History;
import com.debtlens.backend.entity.AnalysisJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Analysis_Status_HistoryRepository
        extends JpaRepository<Analysis_Status_History, Long> {

    // Returns one analysis job's lifecycle events in chronological order.
    List<Analysis_Status_History>
    findByAnalysisJobAnalysisIdOrderByTimestampAsc(Long analysisId);

    // Returns all status history records with relationships pre-fetched, newest first.
    @Query("SELECT ash FROM Analysis_Status_History ash " +
           "LEFT JOIN FETCH ash.analysisJob aj " +
           "LEFT JOIN FETCH aj.repository r " +
           "LEFT JOIN FETCH r.company c " +
           "LEFT JOIN FETCH aj.startedBy u " +
           "ORDER BY ash.timestamp DESC")
    List<Analysis_Status_History> findAllLogsWithDetails();

    // Returns status history records matching a specific status with relationships pre-fetched, newest first.
    @Query("SELECT ash FROM Analysis_Status_History ash " +
           "LEFT JOIN FETCH ash.analysisJob aj " +
           "LEFT JOIN FETCH aj.repository r " +
           "LEFT JOIN FETCH r.company c " +
           "LEFT JOIN FETCH aj.startedBy u " +
           "WHERE ash.status = :status " +
           "ORDER BY ash.timestamp DESC")
    List<Analysis_Status_History> findByStatusWithDetails(@Param("status") AnalysisJobStatus status);
}
