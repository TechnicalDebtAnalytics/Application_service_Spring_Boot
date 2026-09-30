package com.debtlens.backend.dto.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisProgressMessage {
    private Long jobId;
    private Long repositoryId;
    private String repositoryName;
    private String branch;
    private String status;
    private Integer totalClasses;
    private String message;
    private LocalDateTime timestamp;
}
