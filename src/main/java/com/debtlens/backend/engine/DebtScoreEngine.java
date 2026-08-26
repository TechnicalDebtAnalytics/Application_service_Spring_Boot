package com.debtlens.backend.engine;

import com.debtlens.backend.dto.messaging.MLSatdDetectionDTO;
import com.debtlens.backend.entity.Class_Metrics;

import java.util.List;

/**
 * Engine responsible for evaluating and calculating class-level technical debt scores,
 * health classifications, and risk levels based on ML predictions and code metrics.
 */
public interface DebtScoreEngine {

    /**
     * Calculates the composite technical debt score and risk categorization.
     *
     * @param metrics        The extracted Class_Metrics entity containing OO and Git metrics.
     * @param bugProbability The ML bug prediction defect probability in [0.0, 1.0].
     * @param satdDetections The list of classified comments (both debt and non-debt).
     * @return DebtAssessment containing the composite score, health classification, and risk level.
     */
    DebtAssessment calculateDebtScore(
            Class_Metrics metrics,
            Double bugProbability,
            List<MLSatdDetectionDTO> satdDetections
    );
}
