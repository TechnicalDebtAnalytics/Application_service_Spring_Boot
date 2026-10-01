package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.AdminCompanyResponseDTO;
import com.debtlens.backend.dto.response.AdminUserResponseDTO;
import com.debtlens.backend.dto.response.AnalysisResponseDTO;
import com.debtlens.backend.entity.*;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.repository.*;
import com.debtlens.backend.service.impl.AdminCompanyServiceImpl;
import com.debtlens.backend.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminServicesTest {
    private UserRepository users;
    private CompanyRepository companies;
    private MemberRepository members;
    private RepositoryRepository repositories;
    private Super_AdminRepository admins;
    private Analysis_JobRepository jobs;
    private Analysis_Status_HistoryRepository history;
    private Class_MetricsRepository metrics;
    private AdminUserServiceImpl userService;
    private AdminCompanyServiceImpl companyService;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class); companies = mock(CompanyRepository.class);
        members = mock(MemberRepository.class); repositories = mock(RepositoryRepository.class);
        admins = mock(Super_AdminRepository.class); jobs = mock(Analysis_JobRepository.class);
        history = mock(Analysis_Status_HistoryRepository.class); metrics = mock(Class_MetricsRepository.class);
        userService = new AdminUserServiceImpl(users, admins, members);
        companyService = new AdminCompanyServiceImpl(companies, members, repositories, admins, jobs,
                history, metrics, users, userService);
    }

    @Test
    void mapsEveryUserAffiliationAndPrefersSuperAdminWithinOneCompany() {
        User user = user(1L, "Ada", "Admin");
        Company alpha = company(10L, "Alpha", user); Company beta = company(11L, "Beta", user);
        when(admins.findByUserUserIdIn(List.of(1L))).thenReturn(List.of(admin(user, alpha)));
        when(members.findByUserUserIdIn(List.of(1L))).thenReturn(List.of(member(user, alpha), member(user, beta)));

        AdminUserResponseDTO result = userService.mapUsers(new PageImpl<>(List.of(user)), null).getContent().get(0);

        assertEquals(2, result.affiliations().size());
        assertEquals("Super Admin", result.affiliations().get(0).role());
        assertEquals("Member", result.affiliations().get(1).role());
    }

    @Test
    @SuppressWarnings("unchecked")
    void companyPageUsesUniqueUserCountIncludingAdmins() {
        User owner = user(1L, "Ada", "Lovelace"); Company company = company(10L, "Acme", owner);
        var pageable = PageRequest.of(0, 20);
        when(companies.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(company), pageable, 1));
        when(admins.findByCompanyCompanyIdIn(List.of(10L))).thenReturn(List.of(admin(owner, company)));
        when(members.findByCompanyCompanyIdIn(List.of(10L))).thenReturn(List.of(member(owner, company)));
        when(repositories.findByCompanyCompanyIdIn(List.of(10L))).thenReturn(List.of(repository(100L, company)));

        AdminCompanyResponseDTO result = companyService.getAllCompanies(null, null, null, pageable).getContent().get(0);

        assertEquals(1, result.totalUsers());
        assertEquals(1, result.totalRepositories());
    }

    @Test
    @SuppressWarnings("unchecked")
    void analysisPageUsesBatchClassCounts() {
        User owner = user(1L, "Ada", "Lovelace"); Company company = company(10L, "Acme", owner);
        Analysis_Job job = job(1000L, repository(100L, company), owner); var pageable = PageRequest.of(0, 20);
        when(jobs.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(job), pageable, 1));
        Class_MetricsRepository.AnalysisClassCount count = mock(Class_MetricsRepository.AnalysisClassCount.class);
        when(count.getAnalysisId()).thenReturn(1000L); when(count.getClassCount()).thenReturn(4L);
        when(metrics.countByAnalysisIds(List.of(1000L))).thenReturn(List.of(count));

        AnalysisResponseDTO result = companyService.getAllAnalysisJobs(null, null, null, null,
                null, null, pageable).getContent().get(0);

        assertEquals(4, result.totalClassesAnalyzed());
        assertEquals("Acme", result.companyName());
    }

    @Test
    void rejectsUnknownAnalysisStatus() {
        assertThrows(BadRequestException.class, () -> companyService.getAllAnalysisJobs(null, null, null,
                "UNKNOWN", null, null, PageRequest.of(0, 20)));
    }

    private static User user(Long id, String first, String last) {
        User value = new User(); value.setUserId(id); value.setFirstName(first); value.setLastName(last);
        value.setEmail("user" + id + "@example.com"); value.setGithubUsername("gh" + id); value.setEmailVerified(true);
        return value;
    }
    private static Company company(Long id, String name, User owner) {
        Company value = new Company(); value.setCompanyId(id); value.setCompanyName(name);
        value.setGithubOrganizationUrl("https://github.com/" + name.toLowerCase()); value.setCreatedBy(owner); return value;
    }
    private static Super_Admin admin(User user, Company company) { Super_Admin value = new Super_Admin(); value.setUser(user); value.setCompany(company); return value; }
    private static Member member(User user, Company company) { Member value = new Member(); value.setUser(user); value.setCompany(company); return value; }
    private static com.debtlens.backend.entity.Repository repository(Long id, Company company) {
        var value = new com.debtlens.backend.entity.Repository(); value.setRepositoryId(id); value.setRepositoryName("repo");
        value.setRepositoryUrl("https://github.com/acme/repo"); value.setDefaultBranch("main"); value.setCompany(company); return value;
    }
    private static Analysis_Job job(Long id, com.debtlens.backend.entity.Repository repository, User user) {
        Analysis_Job value = new Analysis_Job(); value.setAnalysisId(id); value.setRepository(repository);
        value.setStartedBy(user); value.setStatus(AnalysisJobStatus.COMPLETED); return value;
    }
}
