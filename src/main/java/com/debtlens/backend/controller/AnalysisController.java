package com.debtlens.backend.controller;

import com.debtlens.backend.dto.response.AnalysisResponseDTO;
import com.debtlens.backend.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Start a new analysis job for a repository.
     */
    @PostMapping("/repositories/{repositoryId}/analysis")
    public ResponseEntity<AnalysisResponseDTO> startRepositoryAnalysis(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String branch
    ) {
        AnalysisResponseDTO response = analysisService.startAnalysis(repositoryId, branch);
        return ResponseEntity.ok(response);
    }



    /**
     * Get analysis job status and metadata by analysis ID.
     */
    @GetMapping("/analysis/{analysisId}")
    public ResponseEntity<AnalysisResponseDTO> getAnalysisJob(
            @PathVariable Long analysisId
    ) {
        AnalysisResponseDTO response = analysisService.getAnalysisJob(analysisId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all analysis jobs for a given repository.
     */
    @GetMapping("/repositories/{repositoryId}/analysis")
    public ResponseEntity<List<AnalysisResponseDTO>> getRepositoryAnalysisHistory(
            @PathVariable Long repositoryId
    ) {
        List<AnalysisResponseDTO> history = analysisService.getRepositoryAnalysisHistory(repositoryId);
        return ResponseEntity.ok(history);
    }
}