package com.debtlens.backend.entity;

import jakarta.persistence.*;

/**
 * Stores the ML model's bug-proneness probability for one Class_Metrics record.
 *
 * Bug_Prediction references Class_Metrics rather than Analysis_Job directly
 * because the prediction applies to one analyzed class. Its Analysis_Job
 * remains reachable through Class_Metrics.
 *
 * The actual probability will later be supplied by the ML result workflow.
 */
@Entity
@Table(name = "bug_predictions")
public class Bug_Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prediction_id")
    private Long predictionId;

    // One class metric vector receives one authoritative bug prediction.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false, unique = true)
    private Class_Metrics classMetrics;

    // Probability uses the normalized ML range: 0.0 is lowest and 1.0 highest.
    @Column(name = "probability_score", nullable = false)
    private Double probabilityScore;

    public Long getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(Long predictionId) {
        this.predictionId = predictionId;
    }

    public Class_Metrics getClassMetrics() {
        return classMetrics;
    }

    public void setClassMetrics(Class_Metrics classMetrics) {
        this.classMetrics = classMetrics;
    }

    public Double getProbabilityScore() {
        return probabilityScore;
    }

    public void setProbabilityScore(Double probabilityScore) {
        this.probabilityScore = probabilityScore;
    }
}
