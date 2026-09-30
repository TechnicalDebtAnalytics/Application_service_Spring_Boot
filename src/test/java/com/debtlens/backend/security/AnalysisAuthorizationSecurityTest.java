package com.debtlens.backend.security;

import com.debtlens.backend.entity.Analysis_Job;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.integration.rabbitmq.AnalysisJobProducer;
import com.debtlens.backend.integration.rabbitmq.MLJobProducer;
import com.debtlens.backend.repository.Analysis_JobRepository;
import com.debtlens.backend.repository.Analysis_Status_HistoryRepository;
import com.debtlens.backend.repository.Class_CommentRepository;
import com.debtlens.backend.repository.Class_MetricsRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.service.impl.AnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisAuthorizationSecurityTest {

    private Analysis_JobRepository analysisJobRepository;
    private Analysis_Status_HistoryRepository statusHistoryRepository;
    private RepositoryRepository repositoryRepository;
    private AnalysisJobProducer analysisJobProducer;
    private Auth0UserService auth0UserService;
    private RepositoryAccessService repositoryAccessService;
    private AnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        analysisJobRepository = mock(Analysis_JobRepository.class);
        statusHistoryRepository = mock(Analysis_Status_HistoryRepository.class);
        repositoryRepository = mock(RepositoryRepository.class);
        analysisJobProducer = mock(AnalysisJobProducer.class);
        auth0UserService = mock(Auth0UserService.class);
        repositoryAccessService = mock(RepositoryAccessService.class);

        service = new AnalysisServiceImpl(
                analysisJobRepository,
                statusHistoryRepository,
                mock(Class_MetricsRepository.class),
                mock(Class_CommentRepository.class),
                analysisJobProducer,
                mock(MLJobProducer.class),
                auth0UserService,
                repositoryAccessService
        );
    }

    @Test
    void sec08_unassignedUserCannotStartAnalysisForUnrelatedRepository() {
        User repositoryOwner = user(10L, "Repository", "Owner");
        User unrelatedUser = user(20L, "Unrelated", "User");
        Company company = new Company();
        company.setCompanyId(30L);
        company.setCompanyName("Owner Company");
        company.setCreatedBy(repositoryOwner);

        Repository repository = new Repository();
        repository.setRepositoryId(40L);
        repository.setRepositoryName("private-repository");
        repository.setRepositoryUrl("https://github.com/owner/private-repository");
        repository.setDefaultBranch("main");
        repository.setCompany(company);

        when(repositoryAccessService.requireRepositoryWriteAccess(40L))
                .thenThrow(new AccessDeniedException("denied"));
        when(analysisJobRepository.save(any(Analysis_Job.class))).thenAnswer(invocation -> {
            Analysis_Job job = invocation.getArgument(0);
            job.setAnalysisId(50L);
            return job;
        });

        assertAll(
                () -> assertThrows(
                        AccessDeniedException.class,
                        () -> service.startAnalysis(40L, "main")
                ),
                () -> verify(analysisJobRepository, never()).save(any(Analysis_Job.class)),
                () -> verify(statusHistoryRepository, never()).save(any()),
                () -> verify(analysisJobProducer, never()).publishAnalysisJob(any())
        );
    }

    @Test
    void assignedMemberCanStartAnalysisForAssignedRepository() {
        User repositoryOwner = user(10L, "Repository", "Owner");
        User memberUser = user(20L, "Member", "User");
        Company company = new Company();
        company.setCompanyId(30L);
        company.setCompanyName("Owner Company");
        company.setCreatedBy(repositoryOwner);

        Repository repository = new Repository();
        repository.setRepositoryId(40L);
        repository.setRepositoryName("team-repository");
        repository.setRepositoryUrl("https://github.com/owner/team-repository");
        repository.setDefaultBranch("main");
        repository.setCompany(company);

        com.debtlens.backend.entity.Member member = new com.debtlens.backend.entity.Member();
        member.setMemberId(99L);
        member.setUser(memberUser);
        member.setCompany(company);

        com.debtlens.backend.repository.MemberRepository memberRepository = mock(com.debtlens.backend.repository.MemberRepository.class);
        com.debtlens.backend.repository.Repo_AssignmentRepository repoAssignmentRepository = mock(com.debtlens.backend.repository.Repo_AssignmentRepository.class);
        com.debtlens.backend.repository.Super_AdminRepository superAdminRepository = mock(com.debtlens.backend.repository.Super_AdminRepository.class);

        AnalysisServiceImpl serviceWithMembers = new AnalysisServiceImpl(
                analysisJobRepository,
                statusHistoryRepository,
                mock(Class_MetricsRepository.class),
                mock(Class_CommentRepository.class),
                repositoryRepository,
                analysisJobProducer,
                mock(MLJobProducer.class),
                auth0UserService,
                memberRepository,
                repoAssignmentRepository,
                superAdminRepository,
                null
        );

        when(auth0UserService.getAuthenticatedUser()).thenReturn(memberUser);
        when(repositoryRepository.findById(40L)).thenReturn(Optional.of(repository));
        when(memberRepository.findByUserUserIdAndCompanyCompanyId(20L, 30L)).thenReturn(Optional.of(member));
        when(repoAssignmentRepository.existsByMemberMemberIdAndRepositoryRepositoryId(99L, 40L)).thenReturn(true);
        when(analysisJobRepository.save(any(Analysis_Job.class))).thenAnswer(invocation -> {
            Analysis_Job job = invocation.getArgument(0);
            job.setAnalysisId(50L);
            return job;
        });

        com.debtlens.backend.dto.response.AnalysisResponseDTO response = serviceWithMembers.startAnalysis(40L, "main");
        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertEquals(50L, response.analysisId());
        verify(analysisJobProducer).publishAnalysisJob(any());
    }

    private static User user(Long id, String firstName, String lastName) {
        User user = new User();
        user.setUserId(id);
        user.setAuth0UserId("auth0|" + id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail("user" + id + "@example.test");
        user.setGithubUsername("user" + id);
        user.setEmailVerified(true);
        return user;
    }
}
