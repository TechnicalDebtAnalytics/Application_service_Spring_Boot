package com.debtlens.backend.dto.response;

import com.debtlens.backend.entity.AnalysisJobStatus;

import java.time.LocalDateTime;

public record AnalysisStatusHistoryResponseDTO(
        AnalysisJobStatus status,
        String message,
        LocalDateTime timestamp
) {
}
