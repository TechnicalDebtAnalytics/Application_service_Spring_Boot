package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.AdminActivityResponseDTO;
import com.debtlens.backend.dto.response.AdminSearchResultDTO;
import com.debtlens.backend.dto.response.AdminStatsResponseDTO;
import com.debtlens.backend.entity.AnalysisJobStatus;
import com.debtlens.backend.entity.Analysis_Job;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.repository.Analysis_JobRepository;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.service.AdminDashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RepositoryRepository repositoryRepository;
    private final Analysis_JobRepository analysisJobRepository;

    public AdminDashboardServiceImpl(UserRepository userRepository, CompanyRepository companyRepository,
                                     RepositoryRepository repositoryRepository,
                                     Analysis_JobRepository analysisJobRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.repositoryRepository = repositoryRepository;
        this.analysisJobRepository = analysisJobRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponseDTO getStats() {
        return new AdminStatsResponseDTO(userRepository.count(), companyRepository.count(), repositoryRepository.count(),
                analysisJobRepository.count(), analysisJobRepository.countByStatus(AnalysisJobStatus.QUEUED),
                analysisJobRepository.countByStatus(AnalysisJobStatus.RUNNING),
                analysisJobRepository.countByStatus(AnalysisJobStatus.COMPLETED),
                analysisJobRepository.countByStatus(AnalysisJobStatus.FAILED),
                analysisJobRepository.countByStatus(AnalysisJobStatus.CANCELLED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminActivityResponseDTO> getRecentActivity(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        var newest = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AdminActivityResponseDTO> events = new ArrayList<>();
        userRepository.findAll((Specification<User>) null, newest).forEach(user -> events.add(
                new AdminActivityResponseDTO("USER_REGISTERED", "New user registered",
                        displayName(user), "USER", user.getUserId(), user.getCreatedAt())));
        companyRepository.findAll((Specification<Company>) null, newest).forEach(company -> events.add(
                new AdminActivityResponseDTO("COMPANY_REGISTERED", "New company registered",
                        company.getCompanyName(), "COMPANY", company.getCompanyId(), company.getCreatedAt())));
        repositoryRepository.findAll((Specification<Repository>) null, newest).forEach(repository -> events.add(
                new AdminActivityResponseDTO("REPOSITORY_ADDED", "Repository connected",
                        repository.getRepositoryName(), "COMPANY",
                        repository.getCompany() == null ? null : repository.getCompany().getCompanyId(),
                        repository.getCreatedAt())));

        var jobs = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "startedAt"));
        analysisJobRepository.findAll((Specification<Analysis_Job>) null, jobs).forEach(job -> {
            boolean terminal = job.getCompletedAt() != null;
            String type = terminal ? "ANALYSIS_" + job.getStatus().name() : "ANALYSIS_STARTED";
            String title = terminal ? "Analysis " + job.getStatus().name().toLowerCase(Locale.ROOT) : "Analysis started";
            String description = job.getRepository() == null ? "Analysis #" + job.getAnalysisId()
                    : job.getRepository().getRepositoryName();
            events.add(new AdminActivityResponseDTO(type, title, description, "ANALYSIS_JOB",
                    job.getAnalysisId(), terminal ? job.getCompletedAt() : job.getStartedAt()));
        });
        return events.stream().filter(event -> event.occurredAt() != null)
                .sorted(Comparator.comparing(AdminActivityResponseDTO::occurredAt).reversed())
                .limit(safeLimit).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminSearchResultDTO> search(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        int safeLimit = Math.max(1, Math.min(limit, 10));
        String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        PageRequest page = PageRequest.of(0, safeLimit);
        List<AdminSearchResultDTO> results = new ArrayList<>();

        userRepository.findAll((root, cq, cb) -> cb.or(cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern), cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("githubUsername")), pattern)), page).forEach(user -> results.add(
                new AdminSearchResultDTO("USER", user.getUserId(), null, displayName(user), user.getEmail())));
        companyRepository.findAll((root, cq, cb) -> cb.or(cb.like(cb.lower(root.get("companyName")), pattern),
                cb.like(cb.lower(root.get("githubOrganizationUrl")), pattern)), page).forEach(company -> results.add(
                new AdminSearchResultDTO("COMPANY", company.getCompanyId(), null, company.getCompanyName(),
                        company.getGithubOrganizationUrl())));
        repositoryRepository.findAll((root, cq, cb) -> cb.or(cb.like(cb.lower(root.get("repositoryName")), pattern),
                cb.like(cb.lower(root.get("repositoryUrl")), pattern)), page).forEach(repository -> results.add(
                new AdminSearchResultDTO("REPOSITORY", repository.getRepositoryId(),
                        repository.getCompany() == null ? null : repository.getCompany().getCompanyId(),
                        repository.getRepositoryName(), repository.getRepositoryUrl())));
        analysisJobRepository.findAll((root, cq, cb) -> cb.like(root.get("analysisId").as(String.class), pattern), page)
                .forEach(job -> results.add(new AdminSearchResultDTO("ANALYSIS_JOB", job.getAnalysisId(), null,
                        "Analysis #" + job.getAnalysisId(), job.getStatus().name())));
        return results;
    }

    private String displayName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isBlank() ? user.getEmail() : name;
    }
}
