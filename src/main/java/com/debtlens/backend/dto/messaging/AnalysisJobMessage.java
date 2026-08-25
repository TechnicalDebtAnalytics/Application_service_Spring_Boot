package com.debtlens.backend.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisJobMessage {
    private String jobId;
    private String repositoryId;
    private String repositoryUrl;
    private String branch;
}
