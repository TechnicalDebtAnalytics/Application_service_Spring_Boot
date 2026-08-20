package com.debtlens.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Represents one requested analysis of a repository.
 *
 * The application creates and persists this record before publishing work to
 * RabbitMQ. Workers and future service logic will update its lifecycle status.
 */
@Entity
@Table(name = "analysis_jobs")
public class Analysis_Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    // Repository whose source code will be analyzed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    // User who requested the analysis, retained for authorization and auditing.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "started_by", nullable = false)
    private User startedBy;

    // String persistence keeps lifecycle values readable and avoids enum ordinals.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AnalysisJobStatus status = AnalysisJobStatus.QUEUED;

    // Records when the analysis request/job was first created.
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    // Remains null until the job enters a terminal state.
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Establishes the initial lifecycle state and start timestamp before insertion.
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = AnalysisJobStatus.QUEUED;
        }

        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public User getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(User startedBy) {
        this.startedBy = startedBy;
    }

    public AnalysisJobStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisJobStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
