package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.AdminCompanyResponseDTO;
import com.debtlens.backend.dto.response.AdminUserResponseDTO;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;
import com.debtlens.backend.entity.AnalysisJobStatus;
import com.debtlens.backend.entity.Analysis_Job;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.Analysis_JobRepository;
import com.debtlens.backend.repository.Class_MetricsRepository;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.service.impl.AdminCompanyServiceImpl;
import com.debtlens.backend.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminServicesTest {

    private UserRepository userRepository;
    private CompanyRepository companyRepository;
    private MemberRepository memberRepository;
    private RepositoryRepository repositoryRepository;
    private Super_AdminRepository superAdminRepository;
    private Analysis_JobRepository analysisJobRepository;
    private Class_MetricsRepository classMetricsRepository;
    private AdminUserServiceImpl adminUserService;
    private AdminCompanyServiceImpl adminCompanyService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        companyRepository = mock(CompanyRepository.class);
        memberRepository = mock(MemberRepository.class);
        repositoryRepository = mock(RepositoryRepository.class);
        superAdminRepository = mock(Super_AdminRepository.class);
        analysisJobRepository = mock(Analysis_JobRepository.class);
        classMetricsRepository = mock(Class_MetricsRepository.class);
        adminUserService = new AdminUserServiceImpl(userRepository, superAdminRepository, memberRepository);
        adminCompanyService = new AdminCompanyServiceImpl(companyRepository, memberRepository,
                repositoryRepository, superAdminRepository, analysisJobRepository, classMetricsRepository);
    }

    @Test
    void getAllUsers_shouldPreferSuperAdminThenMemberAndHandleUnassignedUser() {
        Company acme = company(10L, "Acme", user(99L, "Owner", "One"));
        Company beta = company(11L, "Beta", user(98L, "Owner", "Two"));
        User admin = user(1L, "Ada", "Admin");
        User memberUser = user(2L, "Mia", "Member");
        User unassigned = user(3L, "Una", "Assigned");
        when(userRepository.findAll()).thenReturn(List.of(admin, memberUser, unassigned));
        when(superAdminRepository.findByUserUserId(1L)).thenReturn(List.of(superAdmin(admin, acme)));
        when(superAdminRepository.findByUserUserId(2L)).thenReturn(List.of());
        when(superAdminRepository.findByUserUserId(3L)).thenReturn(List.of());
        when(memberRepository.findByUserUserId(2L)).thenReturn(List.of(member(memberUser, beta, 20L)));
        when(memberRepository.findByUserUserId(3L)).thenReturn(List.of());

        List<AdminUserResponseDTO> result = adminUserService.getAllUsers();

        assertEquals(List.of("Super Admin", "Member"),
                result.subList(0, 2).stream().map(AdminUserResponseDTO::companyRole).toList());
        assertEquals(List.of("Acme", "Beta"),
                result.subList(0, 2).stream().map(AdminUserResponseDTO::companyName).toList());
        assertEquals(result.get(2).companyRole(), result.get(2).companyName());
        assertFalse(result.get(2).companyRole().isBlank());
    }

    @Test
    void getAllCompanies_shouldMapCountsAndCreator() {
        User owner = user(1L, "Ada", "Lovelace");
        owner.setEmail("ada@example.com");
        Company company = company(10L, "Acme", owner);
        when(companyRepository.findAll()).thenReturn(List.of(company));
        when(repositoryRepository.findByCompanyCompanyId(10L)).thenReturn(List.of(new Repository(), new Repository()));
        when(memberRepository.findByCompanyCompanyId(10L)).thenReturn(List.of(new Member()));

        AdminCompanyResponseDTO result = adminCompanyService.getAllCompanies().get(0);

        assertEquals("Ada Lovelace", result.superAdminName());
        assertEquals("ada@example.com", result.superAdminEmail());
        assertEquals(2, result.totalRepositories());
        assertEquals(1, result.totalMembers());
    }

    @Test
    void getCompanyUsers_shouldDeduplicateSuperAdminWhoAlsoAppearsAsMember() {
        User admin = user(1L, "Ada", "Admin");
        User memberUser = user(2L, "Mia", "Member");
        Company company = company(10L, "Acme", admin);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(superAdminRepository.findByCompanyCompanyId(10L)).thenReturn(List.of(superAdmin(admin, company)));
        when(memberRepository.findByCompanyCompanyId(10L)).thenReturn(List.of(
                member(admin, company, 20L), member(memberUser, company, 21L)));

        List<AdminUserResponseDTO> result = adminCompanyService.getCompanyUsers(10L);

        assertEquals(2, result.size());
        assertEquals(List.of("Super Admin", "Member"), result.stream().map(AdminUserResponseDTO::companyRole).toList());
        assertEquals(List.of(1L, 2L), result.stream().map(AdminUserResponseDTO::userId).toList());
    }

    @Test
    void getCompanyUsers_shouldFailWhenCompanyMissing() {
        when(companyRepository.findById(404L)).thenReturn(Optional.empty());

        assertEquals("Company not found with ID: 404",
                assertThrows(ResourceNotFoundException.class,
                        () -> adminCompanyService.getCompanyUsers(404L)).getMessage());
    }

    @Test
    void getCompanyAnalysisJobs_shouldMapJobAndMetricCount() {
        User owner = user(1L, "Ada", "Lovelace");
        Company company = company(10L, "Acme", owner);
        Repository repository = repository(100L, company, "develop");
        Analysis_Job job = job(1000L, repository, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(analysisJobRepository.findByRepositoryCompanyCompanyIdOrderByStartedAtDesc(10L)).thenReturn(List.of(job));
        when(classMetricsRepository.countByAnalysisJobAnalysisId(1000L)).thenReturn(4);

        AnalysisResponseDTO result = adminCompanyService.getCompanyAnalysisJobs(10L).get(0);

        assertEquals(10L, result.companyId());
        assertEquals("Acme", result.companyName());
        assertEquals("develop", result.branch());
        assertEquals("Ada Lovelace", result.startedByName());
        assertEquals(4, result.totalClassesAnalyzed());
    }

    @Test
    void getCompanyAnalysisJobs_shouldFailWhenCompanyMissing() {
        when(companyRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminCompanyService.getCompanyAnalysisJobs(404L));
    }

    @Test
    void getAllAnalysisJobs_shouldHandleJobWithoutRepositoryOrNamedUser() {
        User user = user(1L, null, null);
        user.setGithubUsername("octocat");
        Analysis_Job job = job(1000L, null, user);
        when(analysisJobRepository.findAllByOrderByStartedAtDesc()).thenReturn(List.of(job));
        when(classMetricsRepository.countByAnalysisJobAnalysisId(1000L)).thenReturn(0);

        AnalysisResponseDTO result = adminCompanyService.getAllAnalysisJobs().get(0);

        assertEquals("main", result.branch());
        assertEquals("octocat", result.startedByName());
        assertEquals(null, result.repositoryId());
        assertEquals(null, result.companyId());
    }

    private static User user(Long id, String first, String last) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail("user" + id + "@example.com");
        user.setGithubUsername("gh" + id);
        user.setEmailVerified(true);
        return user;
    }

    private static Company company(Long id, String name, User owner) {
        Company company = new Company();
        company.setCompanyId(id);
        company.setCompanyName(name);
        company.setGithubOrganizationUrl("https://github.com/" + name.toLowerCase());
        company.setCreatedBy(owner);
        return company;
    }

    private static Super_Admin superAdmin(User user, Company company) {
        Super_Admin admin = new Super_Admin();
        admin.setUser(user);
        admin.setCompany(company);
        return admin;
    }

    private static Member member(User user, Company company, Long id) {
        Member member = new Member();
        member.setMemberId(id);
        member.setUser(user);
        member.setCompany(company);
        return member;
    }

    private static Repository repository(Long id, Company company, String branch) {
        Repository repository = new Repository();
        repository.setRepositoryId(id);
        repository.setRepositoryName("repo");
        repository.setRepositoryUrl("url");
        repository.setDefaultBranch(branch);
        repository.setCompany(company);
        return repository;
    }

    private static Analysis_Job job(Long id, Repository repository, User user) {
        Analysis_Job job = new Analysis_Job();
        job.setAnalysisId(id);
        job.setRepository(repository);
        job.setStartedBy(user);
        job.setStatus(AnalysisJobStatus.COMPLETED);
        job.setStartedAt(LocalDateTime.now());
        return job;
    }
}
