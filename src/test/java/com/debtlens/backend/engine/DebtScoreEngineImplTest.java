package com.debtlens.backend.engine;

import com.debtlens.backend.dto.messaging.MLSatdDetectionDTO;
import com.debtlens.backend.engine.impl.DebtScoreEngineImpl;
import com.debtlens.backend.entity.Class_Metrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebtScoreEngineImplTest {

    private DebtScoreEngine debtScoreEngine;

    @BeforeEach
    void setUp() {
        debtScoreEngine = new DebtScoreEngineImpl();
    }

    @Test
    void calculateDebtScore_withCleanClass_returnsLowDebtAndExcellentHealth() {
        Class_Metrics metrics = new Class_Metrics();
        metrics.setCbo(2);
        metrics.setDit(1);
        metrics.setLcom(2.0);
        metrics.setWmc(5.0);
        metrics.setNumberOfLinesOfCode(30);
        metrics.setCodeChurnUntil(20);
        metrics.setNumberOfAuthorsUntil(1);
        metrics.setNumberOfVersionsUntil(2);

        List<MLSatdDetectionDTO> satd = List.of(
                MLSatdDetectionDTO.builder().comment("// regular helper").category("non_debt").confidenceScore(0.8).isDebt(false).build()
        );

        DebtAssessment result = debtScoreEngine.calculateDebtScore(metrics, 0.05, satd);

        assertNotNull(result);
        assertTrue(result.getTechnicalDebtScore() < 25.0, "Score should be < 25 for clean class, but got: " + result.getTechnicalDebtScore());
        assertEquals("EXCELLENT", result.getHealthScore());
        assertEquals("LOW", result.getRiskLevel());
    }

    @Test
    void calculateDebtScore_withHighDefectAndHighSatd_returnsCriticalRisk() {
        Class_Metrics metrics = new Class_Metrics();
        metrics.setCbo(18);
        metrics.setDit(4);
        metrics.setLcom(80.0);
        metrics.setWmc(60.0);
        metrics.setNumberOfLinesOfCode(800);
        metrics.setCodeChurnUntil(1500);
        metrics.setNumberOfAuthorsUntil(12);
        metrics.setNumberOfVersionsUntil(45);

        List<MLSatdDetectionDTO> satd = List.of(
                MLSatdDetectionDTO.builder().comment("// FIXME: critical memory leak").category("defect_debt").confidenceScore(0.95).isDebt(true).build(),
                MLSatdDetectionDTO.builder().comment("// TODO: refactor architecture").category("code/design_debt").confidenceScore(0.90).isDebt(true).build()
        );

        DebtAssessment result = debtScoreEngine.calculateDebtScore(metrics, 0.85, satd);

        assertNotNull(result);
        assertTrue(result.getTechnicalDebtScore() >= 75.0, "Score should be >= 75 for high defect and high SATD, got: " + result.getTechnicalDebtScore());
        assertEquals("POOR", result.getHealthScore());
        assertEquals("CRITICAL", result.getRiskLevel());
        assertTrue(result.getBugScore() > 25.0);
        assertTrue(result.getSatdScore() > 15.0);
    }

    @Test
    void calculateDebtScore_withNullInputs_handlesGracefully() {
        DebtAssessment result = debtScoreEngine.calculateDebtScore(null, null, null);

        assertNotNull(result);
        assertEquals(0.0, result.getTechnicalDebtScore());
        assertEquals("EXCELLENT", result.getHealthScore());
        assertEquals("LOW", result.getRiskLevel());
    }
}
