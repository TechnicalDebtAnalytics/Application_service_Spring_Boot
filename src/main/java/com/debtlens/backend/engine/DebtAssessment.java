package com.debtlens.backend.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtAssessment {

    /**
     * Overall composite technical debt score in [0.0, 100.0].
     */
    private Double technicalDebtScore;

    /**
     * Health classification: EXCELLENT, GOOD, FAIR, POOR.
     */
    private String healthScore;

    /**
     * Risk level: LOW, MEDIUM, HIGH, CRITICAL.
     */
    private String riskLevel;

    /**
     * Defect & bug risk component score [0.0, 35.0].
     */
    private Double bugScore;

    /**
     * Self-admitted technical debt component score [0.0, 30.0].
     */
    private Double satdScore;

    /**
     * Structural complexity & coupling component score [0.0, 20.0].
     */
    private Double structuralScore;

    /**
     * Git change volatility & churn component score [0.0, 15.0].
     */
    private Double churnScore;
}
