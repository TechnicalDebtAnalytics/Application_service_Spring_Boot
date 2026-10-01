package com.debtlens.backend.controller;

import com.debtlens.backend.dto.response.*;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.service.AdminCompanyService;
import com.debtlens.backend.service.AdminDashboardService;
import com.debtlens.backend.service.AdminUserService;
import com.debtlens.backend.service.SystemHealthService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminController {
    private static final Set<String> COMPANY_SORTS = Set.of("companyId", "companyName", "createdAt");
    private static final Set<String> USER_SORTS = Set.of("userId", "firstName", "lastName", "email", "createdAt");
    private static final Set<String> JOB_SORTS = Set.of("analysisId", "status", "startedAt", "completedAt");

    private final AdminCompanyService adminCompanyService;
    private final AdminUserService adminUserService;
    private final AdminDashboardService adminDashboardService;
    private final SystemHealthService systemHealthService;

    public AdminController(AdminCompanyService adminCompanyService, AdminUserService adminUserService,
                           AdminDashboardService adminDashboardService, SystemHealthService systemHealthService) {
        this.adminCompanyService = adminCompanyService;
        this.adminUserService = adminUserService;
        this.adminDashboardService = adminDashboardService;
        this.systemHealthService = systemHealthService;
    }

    @GetMapping("/companies")
    public ResponseEntity<PagedResponseDTO<AdminCompanyResponseDTO>> getAllCompanies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(PagedResponseDTO.from(adminCompanyService.getAllCompanies(
                q, createdFrom, createdTo, pageable(page, size, sort, COMPANY_SORTS, "createdAt"))));
    }

    @GetMapping("/companies/{companyId}")
    public ResponseEntity<AdminCompanyResponseDTO> getCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(adminCompanyService.getCompany(companyId));
    }

    @GetMapping("/companies/{companyId}/repositories")
    public ResponseEntity<List<RepositoryResponseDTO>> getCompanyRepositories(@PathVariable Long companyId) {
        return ResponseEntity.ok(adminCompanyService.getCompanyRepositories(companyId));
    }

    @GetMapping("/companies/{companyId}/users")
    public ResponseEntity<PagedResponseDTO<AdminUserResponseDTO>> getCompanyUsers(
            @PathVariable Long companyId, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(PagedResponseDTO.from(adminCompanyService.getCompanyUsers(
                companyId, q, pageable(page, size, sort, USER_SORTS, "createdAt"))));
    }

    @GetMapping("/companies/{companyId}/analysis-jobs")
    public ResponseEntity<PagedResponseDTO<AnalysisResponseDTO>> getCompanyAnalysisJobs(
            @PathVariable Long companyId, @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startedAt,desc") String sort) {
        return ResponseEntity.ok(PagedResponseDTO.from(adminCompanyService.getCompanyAnalysisJobs(
                companyId, q, status, pageable(page, size, sort, JOB_SORTS, "startedAt"))));
    }

    @GetMapping("/users")
    public ResponseEntity<PagedResponseDTO<AdminUserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String q, @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String role, @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(PagedResponseDTO.from(adminUserService.getAllUsers(q, companyId, role,
                emailVerified, pageable(page, size, sort, USER_SORTS, "createdAt"))));
    }

    @GetMapping("/analysis-jobs")
    public ResponseEntity<PagedResponseDTO<AnalysisResponseDTO>> getAllAnalysisJobs(
            @RequestParam(required = false) String q, @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long repositoryId, @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startedTo,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startedAt,desc") String sort) {
        return ResponseEntity.ok(PagedResponseDTO.from(adminCompanyService.getAllAnalysisJobs(q, companyId,
                repositoryId, status, startedFrom, startedTo, pageable(page, size, sort, JOB_SORTS, "startedAt"))));
    }

    @GetMapping("/analysis-jobs/{analysisId}")
    public ResponseEntity<AdminAnalysisJobDetailDTO> getAnalysisJob(@PathVariable Long analysisId) {
        return ResponseEntity.ok(adminCompanyService.getAnalysisJob(analysisId));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponseDTO> getSystemStats() {
        return ResponseEntity.ok(adminDashboardService.getStats());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<AdminActivityResponseDTO>> getActivity(@RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(adminDashboardService.getRecentActivity(Math.max(1, Math.min(limit, 50))));
    }

    @GetMapping("/search")
    public ResponseEntity<List<AdminSearchResultDTO>> search(@RequestParam String q,
                                                              @RequestParam(defaultValue = "5") int limit) {
        if (q == null || q.trim().length() < 2) throw new BadRequestException("Search query must contain at least 2 characters");
        return ResponseEntity.ok(adminDashboardService.search(q, Math.max(1, Math.min(limit, 10))));
    }

    @GetMapping("/health")
    public ResponseEntity<SystemHealthResponseDTO> getSystemHealth() {
        return ResponseEntity.ok(systemHealthService.getSystemHealth());
    }

    private Pageable pageable(int page, int size, String sort, Set<String> allowedFields, String fallback) {
        if (page < 0) throw new BadRequestException("Page must be zero or greater");
        if (size < 1 || size > 100) throw new BadRequestException("Page size must be between 1 and 100");
        String[] parts = sort == null ? new String[0] : sort.split(",", 2);
        String field = parts.length > 0 && allowedFields.contains(parts[0]) ? parts[0] : fallback;
        if (parts.length > 0 && !parts[0].isBlank() && !allowedFields.contains(parts[0])) {
            throw new BadRequestException("Unsupported sort field: " + parts[0]);
        }
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}
