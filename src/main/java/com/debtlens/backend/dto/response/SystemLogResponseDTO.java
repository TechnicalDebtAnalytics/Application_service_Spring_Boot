package com.debtlens.backend.dto.response;

import java.time.LocalDateTime;

public record SystemLogResponseDTO(
        Long statusHistoryId,
        Long analysisId,
        String status,
        String message,
        LocalDateTime timestamp,
        Long repositoryId,
        String repositoryName,
        Long companyId,
        String companyName,
        Long startedByUserId,
        String startedByUserName
) {
}
