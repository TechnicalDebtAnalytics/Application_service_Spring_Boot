package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.AdminCompanyResponseDTO;
import com.debtlens.backend.dto.response.AdminUserResponseDTO;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;
import com.debtlens.backend.dto.response.SystemLogResponseDTO;
import com.debtlens.backend.entity.AnalysisJobStatus;
import com.debtlens.backend.entity.Analysis_Job;
import com.debtlens.backend.entity.Analysis_Status_History;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.Analysis_JobRepository;
import com.debtlens.backend.repository.Analysis_Status_HistoryRepository;
import com.debtlens.backend.repository.Class_MetricsRepository;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import com.debtlens.backend.service.AdminCompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminCompanyServiceImpl implements AdminCompanyService {

    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;
    private final RepositoryRepository repositoryRepository;
    private final Super_AdminRepository superAdminRepository;
    private final Analysis_JobRepository analysisJobRepository;
    private final Class_MetricsRepository classMetricsRepository;
    private final Analysis_Status_HistoryRepository statusHistoryRepository;

    public AdminCompanyServiceImpl(
            CompanyRepository companyRepository,
            MemberRepository memberRepository,
            RepositoryRepository repositoryRepository,
            Super_AdminRepository superAdminRepository,
            Analysis_JobRepository analysisJobRepository,
            Class_MetricsRepository classMetricsRepository,
            Analysis_Status_HistoryRepository statusHistoryRepository
    ) {
        this.companyRepository = companyRepository;
        this.memberRepository = memberRepository;
        this.repositoryRepository = repositoryRepository;
        this.superAdminRepository = superAdminRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.classMetricsRepository = classMetricsRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminCompanyResponseDTO> getAllCompanies() {

        List<Company> companies = companyRepository.findAll();

        return companies.stream()
                .map(company -> {

                    String superAdminName =
                            company.getCreatedBy().getFirstName()
                                    + " "
                                    + company.getCreatedBy().getLastName();

                    String superAdminEmail =
                            company.getCreatedBy().getEmail();

                    int totalRepositories =
                            repositoryRepository
                                    .findByCompanyCompanyId(
                                            company.getCompanyId()
                                    )
                                    .size();

                    int totalMembers =
                            memberRepository
                                    .findByCompanyCompanyId(
                                            company.getCompanyId()
                                    )
                                    .size();

                    return new AdminCompanyResponseDTO(
                            company.getCompanyId(),
                            company.getCompanyName(),
                            company.getGithubOrganizationUrl(),
                            superAdminName,
                            superAdminEmail,
                            totalRepositories,
                            totalMembers,
                            company.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponseDTO> getCompanyUsers(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        List<AdminUserResponseDTO> result = new ArrayList<>();
        Set<Long> processedUserIds = new HashSet<>();

        // 1. Super Admins of this company
        List<Super_Admin> superAdmins = superAdminRepository.findByCompanyCompanyId(companyId);
        for (Super_Admin sa : superAdmins) {
            User user = sa.getUser();
            if (user != null && !processedUserIds.contains(user.getUserId())) {
                processedUserIds.add(user.getUserId());
                result.add(new AdminUserResponseDTO(
                        user.getUserId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getGithubUsername(),
                        user.getEmailVerified(),
                        "Super Admin",
                        company.getCompanyName(),
                        sa.getCreatedAt() != null ? sa.getCreatedAt() : user.getCreatedAt()
                ));
            }
        }

        // 2. Members of this company
        List<Member> members = memberRepository.findByCompanyCompanyId(companyId);
        for (Member m : members) {
            User user = m.getUser();
            if (user != null && !processedUserIds.contains(user.getUserId())) {
                processedUserIds.add(user.getUserId());
                result.add(new AdminUserResponseDTO(
                        user.getUserId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getGithubUsername(),
                        user.getEmailVerified(),
                        "Member",
                        company.getCompanyName(),
                        m.getCreatedAt() != null ? m.getCreatedAt() : user.getCreatedAt()
                ));
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisResponseDTO> getCompanyAnalysisJobs(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        List<Analysis_Job> jobs = analysisJobRepository.findByRepositoryCompanyCompanyIdOrderByStartedAtDesc(companyId);

        return jobs.stream()
                .map(job -> {
                    String branch = job.getRepository() != null ? job.getRepository().getDefaultBranch() : "main";
                    int count = classMetricsRepository.countByAnalysisJobAnalysisId(job.getAnalysisId());

                    String userName = null;
                    Long userId = null;
                    if (job.getStartedBy() != null) {
                        userId = job.getStartedBy().getUserId();
                        userName = (job.getStartedBy().getFirstName() != null ? job.getStartedBy().getFirstName() + " " : "")
                                + (job.getStartedBy().getLastName() != null ? job.getStartedBy().getLastName() : "");
                        if (userName.isBlank()) {
                            userName = job.getStartedBy().getGithubUsername();
                        }
                    }

                    Repository repo = job.getRepository();
                    return new AnalysisResponseDTO(
                            job.getAnalysisId(),
                            repo != null ? repo.getRepositoryId() : null,
                            repo != null ? repo.getRepositoryName() : null,
                            repo != null ? repo.getRepositoryUrl() : null,
                            company.getCompanyId(),
                            company.getCompanyName(),
                            branch,
                            userId,
                            userName != null ? userName.trim() : null,
                            job.getStatus(),
                            job.getStartedAt(),
                            job.getCompletedAt(),
                            count
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisResponseDTO> getAllAnalysisJobs() {
        List<Analysis_Job> jobs = analysisJobRepository.findAllByOrderByStartedAtDesc();

        return jobs.stream()
                .map(job -> {
                    String branch = job.getRepository() != null ? job.getRepository().getDefaultBranch() : "main";
                    int count = classMetricsRepository.countByAnalysisJobAnalysisId(job.getAnalysisId());

                    String userName = null;
                    Long userId = null;
                    if (job.getStartedBy() != null) {
                        userId = job.getStartedBy().getUserId();
                        userName = (job.getStartedBy().getFirstName() != null ? job.getStartedBy().getFirstName() + " " : "")
                                + (job.getStartedBy().getLastName() != null ? job.getStartedBy().getLastName() : "");
                        if (userName.isBlank()) {
                            userName = job.getStartedBy().getGithubUsername();
                        }
                    }

                    Repository repo = job.getRepository();
                    Company company = repo != null ? repo.getCompany() : null;

                    return new AnalysisResponseDTO(
                            job.getAnalysisId(),
                            repo != null ? repo.getRepositoryId() : null,
                            repo != null ? repo.getRepositoryName() : null,
                            repo != null ? repo.getRepositoryUrl() : null,
                            company != null ? company.getCompanyId() : null,
                            company != null ? company.getCompanyName() : null,
                            branch,
                            userId,
                            userName != null ? userName.trim() : null,
                            job.getStatus(),
                            job.getStartedAt(),
                            job.getCompletedAt(),
                            count
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemLogResponseDTO> getSystemLogs(String statusFilter) {
        List<Analysis_Status_History> historyList;

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter.trim())) {
            try {
                AnalysisJobStatus statusEnum = AnalysisJobStatus.valueOf(statusFilter.trim().toUpperCase());
                historyList = statusHistoryRepository.findByStatusWithDetails(statusEnum);
            } catch (IllegalArgumentException e) {
                historyList = statusHistoryRepository.findAllLogsWithDetails();
            }
        } else {
            historyList = statusHistoryRepository.findAllLogsWithDetails();
        }

        return historyList.stream()
                .map(history -> {
                    Analysis_Job job = history.getAnalysisJob();
                    Repository repo = job != null ? job.getRepository() : null;
                    Company company = repo != null ? repo.getCompany() : null;

                    String userName = null;
                    Long userId = null;
                    if (job != null && job.getStartedBy() != null) {
                        userId = job.getStartedBy().getUserId();
                        userName = (job.getStartedBy().getFirstName() != null ? job.getStartedBy().getFirstName() + " " : "")
                                + (job.getStartedBy().getLastName() != null ? job.getStartedBy().getLastName() : "");
                        if (userName.isBlank()) {
                            userName = job.getStartedBy().getGithubUsername();
                        }
                    }

                    return new SystemLogResponseDTO(
                            history.getStatusHistoryId(),
                            job != null ? job.getAnalysisId() : null,
                            history.getStatus() != null ? history.getStatus().name() : null,
                            history.getMessage(),
                            history.getTimestamp(),
                            repo != null ? repo.getRepositoryId() : null,
                            repo != null ? repo.getRepositoryName() : null,
                            company != null ? company.getCompanyId() : null,
                            company != null ? company.getCompanyName() : null,
                            userId,
                            userName != null ? userName.trim() : null
                    );
                })
                .toList();
    }
}