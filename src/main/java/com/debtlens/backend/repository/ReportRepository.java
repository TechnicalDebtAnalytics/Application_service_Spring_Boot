package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // Returns one analysis job's generated reports, newest report first.
    List<Report>
    findByAnalysisJobAnalysisIdOrderByGeneratedAtDesc(Long analysisId);
}
