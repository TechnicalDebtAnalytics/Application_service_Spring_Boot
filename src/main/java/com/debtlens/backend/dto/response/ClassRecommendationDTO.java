package com.debtlens.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRecommendationDTO {
    private Long classId;
    private String className;
    private String filePath;
    private int startLine;
    private int endLine;
    private int numberOfLinesOfCode;

    private Double technicalDebtScore;
    private String healthScore;
    private String riskLevel;
    private Double bugProbability;

    private int refactorPriorityRank; // 1 = highest priority to refactor first

    @Builder.Default
    private List<String> primaryDrivers = new ArrayList<>();

    @Builder.Default
    private List<RefactoringActionDTO> recommendedActions = new ArrayList<>();
}
