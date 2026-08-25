package com.debtlens.backend.dto.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResultDTO {
    private String jobId;
    private String repositoryId;
    private String status;
    private RepositoryMetricsDTO repositoryMetrics;
    private String comments;
    private String error;
}
