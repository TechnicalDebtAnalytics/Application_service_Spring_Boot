package com.debtlens.backend.service;

import com.debtlens.backend.dto.messaging.AnalysisResultDTO;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;

import java.util.List;

public interface AnalysisService {

    AnalysisResponseDTO startAnalysis(Long repositoryId, String branch);

    AnalysisResponseDTO cancelAnalysis(Long analysisId);

    AnalysisResponseDTO cancelRepositoryAnalysis(Long repositoryId);

    AnalysisResponseDTO getAnalysisJob(Long analysisId);

    List<AnalysisResponseDTO> getRepositoryAnalysisHistory(Long repositoryId);

    List<AnalysisResponseDTO> getCompanyAnalysisHistory(Long companyId);

    void processAnalysisResult(AnalysisResultDTO result);
}