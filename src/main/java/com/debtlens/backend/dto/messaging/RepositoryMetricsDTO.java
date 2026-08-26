package com.debtlens.backend.dto.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryMetricsDTO {
    private String repositoryId;
    private String repositoryName;
    private List<ClassMetricsDTO> classMetrics;
    private GitMetricsDTO gitMetrics;
}
