package com.debtlens.backend.service;

import com.debtlens.backend.dto.messaging.AnalysisResultDTO;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;

import java.util.List;

public interface AnalysisService {

    AnalysisResponseDTO startAnalysis(Long repositoryId, String branch);

    AnalysisResponseDTO getAnalysisJob(Long analysisId);

    List<AnalysisResponseDTO> getRepositoryAnalysisHistory(Long repositoryId);

    void processAnalysisResult(AnalysisResultDTO result);
}