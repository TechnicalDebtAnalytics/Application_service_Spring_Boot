package com.debtlens.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Records one lifecycle-status event for an Analysis_Job.
 *
 * Future service or worker-result handling should update Analysis_Job.status
 * and insert the corresponding history record in the same transaction so the
 * current status and its history remain consistent.
 */
@Entity
@Table(name = "analysis_status_history")
public class Analysis_Status_History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_history_id")
    private Long statusHistoryId;

    // The analysis job whose lifecycle produced this history event.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis_Job analysisJob;

    // Reuses the job's enum so current status and historical status values
    // cannot evolve into separate, incompatible vocabularies.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AnalysisJobStatus status;

    // Optional detail about the transition, worker progress, failure, or cancellation.
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    // Records when this status event occurred and supports chronological history.
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // Ensures Java-created history records receive an event timestamp before insertion.
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public Long getStatusHistoryId() {
        return statusHistoryId;
    }

    public void setStatusHistoryId(Long statusHistoryId) {
        this.statusHistoryId = statusHistoryId;
    }

    public Analysis_Job getAnalysisJob() {
        return analysisJob;
    }

    public void setAnalysisJob(Analysis_Job analysisJob) {
        this.analysisJob = analysisJob;
    }

    public AnalysisJobStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisJobStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
