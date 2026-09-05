package com.debtlens.backend.dto.response;

import com.debtlens.backend.entity.AnalysisJobStatus;

import java.time.LocalDateTime;

public record AnalysisResponseDTO(
        Long analysisId,
        Long repositoryId,
        String repositoryName,
        String repositoryUrl,
        Long companyId,
        String companyName,
        String branch,
        Long startedByUserId,
        String startedByName,
        AnalysisJobStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer totalClassesAnalyzed
) {
}

