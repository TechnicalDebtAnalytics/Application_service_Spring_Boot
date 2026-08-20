package com.debtlens.backend.entity;

import jakarta.persistence.*;

/**
 * Stores the ML classification result for one raw Class_Comment.
 *
 * SATD_Detection references Class_Comment rather than Class_Metrics directly
 * because the classification applies to the individual extracted comment.
 * Class_Metrics and Analysis_Job remain reachable through that relationship.
 *
 * A raw comment is not automatically technical debt. The future ML workflow
 * will supply the category and normalized confidence score.
 */
@Entity
@Table(name = "satd_detections")
public class SATD_Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detection_id")
    private Long detectionId;

    // One raw comment may have at most one current authoritative classification.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false, unique = true)
    private Class_Comment classComment;

    // Stores the ML classification label. Its taxonomy is not yet finalized.
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    // Normalized ML confidence: 0.0 is lowest and 1.0 is highest.
    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    public Long getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(Long detectionId) {
        this.detectionId = detectionId;
    }

    public Class_Comment getClassComment() {
        return classComment;
    }

    public void setClassComment(Class_Comment classComment) {
        this.classComment = classComment;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
