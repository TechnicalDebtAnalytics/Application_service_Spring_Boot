package com.debtlens.backend.engine.impl;

import com.debtlens.backend.dto.messaging.MLSatdDetectionDTO;
import com.debtlens.backend.engine.DebtAssessment;
import com.debtlens.backend.engine.DebtScoreEngine;
import com.debtlens.backend.entity.Class_Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class DebtScoreEngineImpl implements DebtScoreEngine {

    private static final Logger log = LoggerFactory.getLogger(DebtScoreEngineImpl.class);

    // Maximum pillar point allocations (sum to 100.0)
    private static final double MAX_BUG_PILLAR_SCORE = 35.0;
    private static final double MAX_SATD_PILLAR_SCORE = 30.0;
    private static final double MAX_STRUCTURAL_PILLAR_SCORE = 20.0;
    private static final double MAX_CHURN_PILLAR_SCORE = 15.0;

    @Override
    public DebtAssessment calculateDebtScore(
            Class_Metrics metrics,
            Double bugProbability,
            List<MLSatdDetectionDTO> satdDetections
    ) {
        // 1. Bug Defect Risk Pillar (35% max)
        double bugProb = (bugProbability != null) ? Math.max(0.0, Math.min(1.0, bugProbability)) : 0.0;
        double bugScore = Math.round(bugProb * MAX_BUG_PILLAR_SCORE * 10.0) / 10.0;

        // 2. SATD & Comment Health Pillar (30% max)
        double satdScore = calculateSatdScore(satdDetections);

        // 3. Structural Complexity & Coupling Pillar (20% max)
        double structuralScore = calculateStructuralScore(metrics);

        // 4. Git Churn & Volatility Pillar (15% max)
        double churnScore = calculateChurnScore(metrics);

        // 5. Composite Technical Debt Score (0.0 - 100.0)
        double totalScore = bugScore + satdScore + structuralScore + churnScore;
        double clampedScore = Math.max(0.0, Math.min(100.0, Math.round(totalScore * 10.0) / 10.0));

        // 6. Derive Health Score and Risk Level
        String healthScore = deriveHealthScore(clampedScore);
        String riskLevel = deriveRiskLevel(clampedScore, bugProb);

        log.debug("Debt score evaluation: total={}, health={}, risk={}, (bug={}, satd={}, struct={}, churn={})",
                clampedScore, healthScore, riskLevel, bugScore, satdScore, structuralScore, churnScore);

        return DebtAssessment.builder()
                .technicalDebtScore(clampedScore)
                .healthScore(healthScore)
                .riskLevel(riskLevel)
                .bugScore(bugScore)
                .satdScore(satdScore)
                .structuralScore(structuralScore)
                .churnScore(churnScore)
                .build();
    }

    private double calculateSatdScore(List<MLSatdDetectionDTO> satdDetections) {
        if (satdDetections == null || satdDetections.isEmpty()) {
            return 0.0;
        }

        int totalComments = satdDetections.size();
        int debtCount = 0;
        double weightedImpactSum = 0.0;

        for (MLSatdDetectionDTO dto : satdDetections) {
            if (dto.isDebt()) {
                debtCount++;
                double confidence = Math.max(0.0, Math.min(1.0, dto.getConfidenceScore()));
                double severity = getCategorySeverity(dto.getCategory());
                weightedImpactSum += (confidence * severity);
            }
        }

        if (debtCount == 0) {
            return 0.0;
        }

        double densityRatio = (double) debtCount / totalComments;
        // Weighted impact + density scaled to 30.0 max
        double rawSatd = (weightedImpactSum * 8.0) + (densityRatio * 6.0);
        return Math.max(0.0, Math.min(MAX_SATD_PILLAR_SCORE, Math.round(rawSatd * 10.0) / 10.0));
    }

    private double getCategorySeverity(String category) {
        if (category == null) {
            return 0.5;
        }
        String cat = category.toLowerCase(Locale.ROOT).trim();
        if (cat.contains("defect")) {
            return 1.25;
        } else if (cat.contains("design") || cat.contains("code")) {
            return 1.00;
        } else if (cat.contains("requirement")) {
            return 0.85;
        } else if (cat.contains("test") || cat.contains("build") || cat.contains("doc")) {
            return 0.65;
        } else if (cat.contains("non_debt")) {
            return 0.0;
        }
        return 0.70;
    }

    private double calculateStructuralScore(Class_Metrics metrics) {
        if (metrics == null) {
            return 0.0;
        }

        double cboNorm = Math.min(1.0, Math.max(0, metrics.getCbo()) / 15.0);
        double lcomNorm = Math.min(1.0, Math.max(0.0, metrics.getLcom()) / 50.0);
        double wmcNorm = Math.min(1.0, Math.max(0.0, metrics.getWmc()) / 40.0);
        double locNorm = Math.min(1.0, Math.max(0, metrics.getNumberOfLinesOfCode()) / 500.0);

        double compositeStruct = (0.35 * cboNorm) + (0.25 * lcomNorm) + (0.25 * wmcNorm) + (0.15 * locNorm);
        double rawStruct = compositeStruct * MAX_STRUCTURAL_PILLAR_SCORE;
        return Math.max(0.0, Math.min(MAX_STRUCTURAL_PILLAR_SCORE, Math.round(rawStruct * 10.0) / 10.0));
    }

    private double calculateChurnScore(Class_Metrics metrics) {
        if (metrics == null) {
            return 0.0;
        }

        double churnNorm = Math.min(1.0, Math.max(0, metrics.getCodeChurnUntil()) / 1000.0);
        double authorNorm = Math.min(1.0, Math.max(0, metrics.getNumberOfAuthorsUntil()) / 10.0);
        double versionNorm = Math.min(1.0, Math.max(0, metrics.getNumberOfVersionsUntil()) / 30.0);

        double compositeChurn = (0.50 * churnNorm) + (0.30 * authorNorm) + (0.20 * versionNorm);
        double rawChurn = compositeChurn * MAX_CHURN_PILLAR_SCORE;
        return Math.max(0.0, Math.min(MAX_CHURN_PILLAR_SCORE, Math.round(rawChurn * 10.0) / 10.0));
    }

    private String deriveHealthScore(double technicalDebtScore) {
        if (technicalDebtScore < 25.0) {
            return "EXCELLENT";
        } else if (technicalDebtScore < 50.0) {
            return "GOOD";
        } else if (technicalDebtScore < 75.0) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    private String deriveRiskLevel(double technicalDebtScore, double bugProbability) {
        if (technicalDebtScore >= 75.0 || bugProbability >= 0.70) {
            return "CRITICAL";
        } else if (technicalDebtScore >= 50.0 || bugProbability >= 0.45) {
            return "HIGH";
        } else if (technicalDebtScore >= 25.0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
