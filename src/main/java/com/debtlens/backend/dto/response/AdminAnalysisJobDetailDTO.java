package com.debtlens.backend.dto.response;

import java.util.List;

public record AdminAnalysisJobDetailDTO(
        AnalysisResponseDTO job,
        List<AnalysisStatusHistoryResponseDTO> history
) {
}
