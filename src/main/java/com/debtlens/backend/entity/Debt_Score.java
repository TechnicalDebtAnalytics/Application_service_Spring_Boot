package com.debtlens.backend.entity;

import jakarta.persistence.*;

/**
 * Stores the derived technical-debt assessment for one Class_Metrics record.
 *
 * Debt_Score references Class_Metrics directly because the assessment belongs
 * to one analyzed class. Analysis_Job remains reachable through Class_Metrics.
 *
 * Score calculation, aggregation, and risk interpretation belong to future
 * analysis and reporting business logic.
 */
@Entity
@Table(name = "debt_scores")
public class Debt_Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "debt_score_id")
    private Long debtScoreId;

    // One class metric vector receives one current authoritative assessment.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false, unique = true)
    private Class_Metrics classMetrics;

    // Computed class-level debt score; its formula and range are not yet defined.
    @Column(name = "technical_debt_score", nullable = false)
    private Double technicalDebtScore;

    // Remains a string until an authoritative health taxonomy is finalized.
    @Column(name = "health_score", nullable = false, length = 100)
    private String healthScore;

    // Remains a string until authoritative risk-level values are finalized.
    @Column(name = "risk_level", nullable = false, length = 100)
    private String riskLevel;

    public Long getDebtScoreId() {
        return debtScoreId;
    }

    public void setDebtScoreId(Long debtScoreId) {
        this.debtScoreId = debtScoreId;
    }

    public Class_Metrics getClassMetrics() {
        return classMetrics;
    }

    public void setClassMetrics(Class_Metrics classMetrics) {
        this.classMetrics = classMetrics;
    }

    public Double getTechnicalDebtScore() {
        return technicalDebtScore;
    }

    public void setTechnicalDebtScore(Double technicalDebtScore) {
        this.technicalDebtScore = technicalDebtScore;
    }

    public String getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(String healthScore) {
        this.healthScore = healthScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
