package com.debtlens.backend.controller;

import com.debtlens.backend.dto.response.ClassRecommendationDTO;
import com.debtlens.backend.dto.response.ReportResponseDTO;
import com.debtlens.backend.entity.Report;
import com.debtlens.backend.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Generates a complete Technical Debt Report for an analysis job,
     * including repository health grades, score summaries, and prioritized refactoring recommendations.
     */
    @GetMapping("/analysis/{analysisId}/report")
    public ResponseEntity<ReportResponseDTO> getAnalysisReport(@PathVariable Long analysisId) {
        ReportResponseDTO report = reportService.generateReport(analysisId);
        return ResponseEntity.ok(report);
    }

    /**
     * Returns the prioritized refactoring recommendation list ordered with highest-debt classes first.
     */
    @GetMapping("/analysis/{analysisId}/recommendations")
    public ResponseEntity<List<ClassRecommendationDTO>> getRecommendations(@PathVariable Long analysisId) {
        List<ClassRecommendationDTO> recommendations = reportService.getPrioritizedRecommendations(analysisId);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Returns the generation history log for an analysis job from the reports table.
     */
    @GetMapping("/analysis/{analysisId}/report-history")
    public ResponseEntity<List<Report>> getReportHistory(@PathVariable Long analysisId) {
        List<Report> history = reportService.getReportHistory(analysisId);
        return ResponseEntity.ok(history);
    }
}
