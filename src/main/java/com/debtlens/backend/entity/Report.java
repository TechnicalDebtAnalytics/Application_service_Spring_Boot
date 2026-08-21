package com.debtlens.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents one generated report belonging to an Analysis_Job.
 *
 * The approved ER model allows an analysis to have multiple report records,
 * supporting later regeneration without overwriting earlier report entries.
 */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    // The analysis job whose processed results produced this report.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis_Job analysisJob;

    // Records when this particular report record was generated.
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    // Ensures Java-created report records receive a generation timestamp.
    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Analysis_Job getAnalysisJob() {
        return analysisJob;
    }

    public void setAnalysisJob(Analysis_Job analysisJob) {
        this.analysisJob = analysisJob;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
