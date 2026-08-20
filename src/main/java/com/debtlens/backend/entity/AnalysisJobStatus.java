package com.debtlens.backend.entity;

/**
 * Represents the lifecycle state of an analysis job.
 */
public enum AnalysisJobStatus {
    QUEUED,    // Created and waiting for an analysis worker.
    RUNNING,   // Currently being processed.
    COMPLETED, // Finished successfully.
    FAILED,    // Stopped because processing failed.
    CANCELLED  // Explicitly cancelled before successful completion.
}
