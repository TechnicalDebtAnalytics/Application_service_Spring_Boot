package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.*;
import com.debtlens.backend.entity.*;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.*;
import com.debtlens.backend.service.AdminCompanyService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminCompanyServiceImpl implements AdminCompanyService {
    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;
    private final RepositoryRepository repositoryRepository;
    private final Super_AdminRepository superAdminRepository;
    private final Analysis_JobRepository analysisJobRepository;
    private final Analysis_Status_HistoryRepository statusHistoryRepository;
    private final Class_MetricsRepository classMetricsRepository;
    private final UserRepository userRepository;
    private final AdminUserServiceImpl adminUserService;

    public AdminCompanyServiceImpl(CompanyRepository companyRepository, MemberRepository memberRepository,
                                   RepositoryRepository repositoryRepository, Super_AdminRepository superAdminRepository,
                                   Analysis_JobRepository analysisJobRepository,
                                   Analysis_Status_HistoryRepository statusHistoryRepository,
                                   Class_MetricsRepository classMetricsRepository, UserRepository userRepository,
                                   AdminUserServiceImpl adminUserService) {
        this.companyRepository = companyRepository;
        this.memberRepository = memberRepository;
        this.repositoryRepository = repositoryRepository;
        this.superAdminRepository = superAdminRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.classMetricsRepository = classMetricsRepository;
        this.userRepository = userRepository;
        this.adminUserService = adminUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminCompanyResponseDTO> getAllCompanies(String query, LocalDate createdFrom,
                                                          LocalDate createdTo, Pageable pageable) {
        Specification<Company> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("companyName")), pattern),
                        cb.like(cb.lower(root.get("githubOrganizationUrl")), pattern)));
            }
            if (createdFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom.atStartOfDay()));
            if (createdTo != null) predicates.add(cb.lessThan(root.get("createdAt"), createdTo.plusDays(1).atStartOfDay()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Company> page = companyRepository.findAll(spec, pageable);
        List<Long> ids = page.getContent().stream().map(Company::getCompanyId).toList();
        Map<Long, Set<Long>> usersByCompany = new HashMap<>();
        Map<Long, Long> repositoriesByCompany = new HashMap<>();
        if (!ids.isEmpty()) {
            memberRepository.findByCompanyCompanyIdIn(ids).forEach(member -> usersByCompany
                    .computeIfAbsent(member.getCompany().getCompanyId(), ignored -> new HashSet<>())
                    .add(member.getUser().getUserId()));
            superAdminRepository.findByCompanyCompanyIdIn(ids).forEach(admin -> usersByCompany
                    .computeIfAbsent(admin.getCompany().getCompanyId(), ignored -> new HashSet<>())
                    .add(admin.getUser().getUserId()));
            repositoryRepository.findByCompanyCompanyIdIn(ids).forEach(repo -> repositoriesByCompany.merge(
                    repo.getCompany().getCompanyId(), 1L, Long::sum));
        }
        List<AdminCompanyResponseDTO> content = page.getContent().stream().map(company -> {
            User owner = company.getCreatedBy();
            String ownerName = owner == null ? "Unknown" : ((owner.getFirstName() == null ? "" : owner.getFirstName())
                    + " " + (owner.getLastName() == null ? "" : owner.getLastName())).trim();
            return new AdminCompanyResponseDTO(company.getCompanyId(), company.getCompanyName(),
                    company.getGithubOrganizationUrl(), ownerName, owner == null ? null : owner.getEmail(),
                    repositoriesByCompany.getOrDefault(company.getCompanyId(), 0L).intValue(),
                    usersByCompany.getOrDefault(company.getCompanyId(), Set.of()).size(), company.getCreatedAt());
        }).toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCompanyResponseDTO getCompany(Long companyId) {
        Company company = requireCompany(companyId);
        Set<Long> userIds = new HashSet<>();
        memberRepository.findByCompanyCompanyId(companyId).forEach(member -> userIds.add(member.getUser().getUserId()));
        superAdminRepository.findByCompanyCompanyId(companyId).forEach(admin -> userIds.add(admin.getUser().getUserId()));
        User owner = company.getCreatedBy();
        String ownerName = owner == null ? "Unknown" : ((owner.getFirstName() == null ? "" : owner.getFirstName())
                + " " + (owner.getLastName() == null ? "" : owner.getLastName())).trim();
        return new AdminCompanyResponseDTO(company.getCompanyId(), company.getCompanyName(),
                company.getGithubOrganizationUrl(), ownerName, owner == null ? null : owner.getEmail(),
                repositoryRepository.findByCompanyCompanyId(companyId).size(), userIds.size(), company.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResponseDTO> getCompanyRepositories(Long companyId) {
        requireCompany(companyId);
        return repositoryRepository.findByCompanyCompanyId(companyId).stream()
                .map(repo -> new RepositoryResponseDTO(repo.getRepositoryId(), repo.getGithubRepositoryId(),
                        repo.getRepositoryName(), repo.getRepositoryUrl(), repo.getDefaultBranch(), repo.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDTO> getCompanyUsers(Long companyId, String query, Pageable pageable) {
        requireCompany(companyId);
        Page<User> users = userRepository.findAll(
                AdminUserServiceImpl.userSpecification(query, companyId, null, null), pageable);
        return adminUserService.mapUsers(users, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnalysisResponseDTO> getCompanyAnalysisJobs(Long companyId, String query, String status,
                                                             Pageable pageable) {
        requireCompany(companyId);
        return getAllAnalysisJobs(query, companyId, null, status, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnalysisResponseDTO> getAllAnalysisJobs(String query, Long companyId, Long repositoryId,
                                                         String status, LocalDate startedFrom, LocalDate startedTo,
                                                         Pageable pageable) {
        AnalysisJobStatus parsedStatus = parseStatus(status);
        Specification<Analysis_Job> spec = (root, cq, cb) -> {
            var repository = root.join("repository", JoinType.LEFT);
            var company = repository.join("company", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(repository.get("repositoryName")), pattern),
                        cb.like(cb.lower(company.get("companyName")), pattern)));
            }
            if (companyId != null) predicates.add(cb.equal(company.get("companyId"), companyId));
            if (repositoryId != null) predicates.add(cb.equal(repository.get("repositoryId"), repositoryId));
            if (parsedStatus != null) predicates.add(cb.equal(root.get("status"), parsedStatus));
            if (startedFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), startedFrom.atStartOfDay()));
            if (startedTo != null) predicates.add(cb.lessThan(root.get("startedAt"), startedTo.plusDays(1).atStartOfDay()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Analysis_Job> page = analysisJobRepository.findAll(spec, pageable);
        Map<Long, Integer> counts = classCounts(page.getContent());
        List<AnalysisResponseDTO> content = page.getContent().stream().map(job -> mapJob(job,
                counts.getOrDefault(job.getAnalysisId(), 0))).toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAnalysisJobDetailDTO getAnalysisJob(Long analysisId) {
        Analysis_Job job = analysisJobRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis job not found with ID: " + analysisId));
        List<AnalysisStatusHistoryResponseDTO> history = statusHistoryRepository
                .findByAnalysisJobAnalysisIdOrderByTimestampAsc(analysisId).stream()
                .map(item -> new AnalysisStatusHistoryResponseDTO(item.getStatus(), item.getMessage(), item.getTimestamp()))
                .toList();
        return new AdminAnalysisJobDetailDTO(mapJob(job,
                classMetricsRepository.countByAnalysisJobAnalysisId(analysisId)), history);
    }

    private Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));
    }

    private AnalysisJobStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return AnalysisJobStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unknown analysis status: " + status);
        }
    }

    private Map<Long, Integer> classCounts(List<Analysis_Job> jobs) {
        List<Long> ids = jobs.stream().map(Analysis_Job::getAnalysisId).toList();
        if (ids.isEmpty()) return Map.of();
        return classMetricsRepository.countByAnalysisIds(ids).stream().collect(Collectors.toMap(
                Class_MetricsRepository.AnalysisClassCount::getAnalysisId,
                count -> count.getClassCount().intValue()));
    }

    private AnalysisResponseDTO mapJob(Analysis_Job job, int classCount) {
        Repository repo = job.getRepository();
        Company company = repo == null ? null : repo.getCompany();
        User startedBy = job.getStartedBy();
        String name = startedBy == null ? null : ((startedBy.getFirstName() == null ? "" : startedBy.getFirstName())
                + " " + (startedBy.getLastName() == null ? "" : startedBy.getLastName())).trim();
        if (name != null && name.isBlank()) name = startedBy.getGithubUsername();
        return new AnalysisResponseDTO(job.getAnalysisId(), repo == null ? null : repo.getRepositoryId(),
                repo == null ? null : repo.getRepositoryName(), repo == null ? null : repo.getRepositoryUrl(),
                company == null ? null : company.getCompanyId(), company == null ? null : company.getCompanyName(),
                repo == null || repo.getDefaultBranch() == null ? "main" : repo.getDefaultBranch(),
                startedBy == null ? null : startedBy.getUserId(), name, job.getStatus(), job.getStartedAt(),
                job.getCompletedAt(), classCount);
    }
}
