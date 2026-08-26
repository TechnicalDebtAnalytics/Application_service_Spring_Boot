package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.ClassRecommendationDTO;
import com.debtlens.backend.dto.response.ReportResponseDTO;

import java.util.List;

public interface ReportService {

    /**
     * Generates a complete Technical Debt Report for an analysis job,
     * including aggregated scores and prioritized refactoring recommendations.
     * Logs the generation timestamp in the reports table.
     */
    ReportResponseDTO generateReport(Long analysisId);

    /**
     * Returns the prioritized list of classes that should be refactored first,
     * ordered by highest technical debt score and risk severity.
     */
    List<ClassRecommendationDTO> getPrioritizedRecommendations(Long analysisId);
}