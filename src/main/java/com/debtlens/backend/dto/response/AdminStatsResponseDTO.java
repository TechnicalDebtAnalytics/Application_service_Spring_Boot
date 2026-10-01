package com.debtlens.backend.dto.response;

public record AdminStatsResponseDTO(
        long totalUsers,
        long totalCompanies,
        long totalRepositories,
        long totalAnalysisJobs,
        long queuedJobs,
        long runningJobs,
        long completedJobs,
        long failedJobs,
        long cancelledJobs
) {
}
