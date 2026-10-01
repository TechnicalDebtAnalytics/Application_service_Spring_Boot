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

    @PostMapping("/repositories/{repositoryId}/analysis")
    public ResponseEntity<AnalysisResponseDTO> startRepositoryAnalysis(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String branch
    ) {
        AnalysisResponseDTO response = analysisService.startAnalysis(repositoryId, branch);
        return ResponseEntity.ok(response);
    }



   
    @PostMapping("/repositories/{repositoryId}/analysis/cancel")
    public ResponseEntity<AnalysisResponseDTO> cancelRepositoryAnalysis(
            @PathVariable Long repositoryId
    ) {
        AnalysisResponseDTO response = analysisService.cancelRepositoryAnalysis(repositoryId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analysis/{analysisId}/cancel")
    public ResponseEntity<AnalysisResponseDTO> cancelAnalysisJob(
            @PathVariable Long analysisId
    ) {
        AnalysisResponseDTO response = analysisService.cancelAnalysis(analysisId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analysis/{analysisId}")
    public ResponseEntity<AnalysisResponseDTO> getAnalysisJob(
            @PathVariable Long analysisId
    ) {
        AnalysisResponseDTO response = analysisService.getAnalysisJob(analysisId);
        return ResponseEntity.ok(response);
    }

  
    @GetMapping("/repositories/{repositoryId}/analysis")
    public ResponseEntity<List<AnalysisResponseDTO>> getRepositoryAnalysisHistory(
            @PathVariable Long repositoryId
    ) {
        List<AnalysisResponseDTO> history = analysisService.getRepositoryAnalysisHistory(repositoryId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/companies/{companyId}/analysis")
    public ResponseEntity<List<AnalysisResponseDTO>> getCompanyAnalysisHistory(
            @PathVariable Long companyId
    ) {
        List<AnalysisResponseDTO> history = analysisService.getCompanyAnalysisHistory(companyId);
        return ResponseEntity.ok(history);
    }
}