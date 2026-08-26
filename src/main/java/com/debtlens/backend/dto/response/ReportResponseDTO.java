package com.debtlens.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDTO {

    private Long reportId;
    private Long analysisId;
    private Long repositoryId;
    private String repositoryName;
    private String branch;
    private LocalDateTime generatedAt;

    private Double overallDebtScore;
    private String overallHealthScore;
    private String overallRiskLevel;

    private int totalClasses;
    private int defectiveClassesCount;
    private int totalSatdComments;

    @Builder.Default
    private List<ClassRecommendationDTO> prioritizedRefactoringList = new ArrayList<>();
}
