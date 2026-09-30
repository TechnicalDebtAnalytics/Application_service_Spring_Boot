package com.debtlens.backend.security;

import com.debtlens.backend.entity.Analysis_Job;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.Analysis_JobRepository;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.Repo_AssignmentRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryAccessServiceTest {

    @Mock private RepositoryRepository repositoryRepository;
    @Mock private Analysis_JobRepository analysisJobRepository;
    @Mock private Super_AdminRepository superAdminRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private Repo_AssignmentRepository repoAssignmentRepository;
    @Mock private Auth0UserService auth0UserService;

    private RepositoryAccessService service;

    @BeforeEach
    void setUp() {
        service = new RepositoryAccessService(
                repositoryRepository,
                analysisJobRepository,
                superAdminRepository,
                memberRepository,
                repoAssignmentRepository,
                auth0UserService
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void companySuperAdminHasRepositoryWriteAccess() {
        Repository repository = repository(10L, 20L);
        User user = user(30L);
        when(repositoryRepository.findById(10L)).thenReturn(Optional.of(repository));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(user);
        when(superAdminRepository.existsByUserUserIdAndCompanyCompanyId(30L, 20L)).thenReturn(true);

        assertEquals(repository, service.requireRepositoryWriteAccess(10L));

        verifyNoInteractions(memberRepository, repoAssignmentRepository);
    }

    @Test
    void assignedMemberHasRepositoryWriteAccess() {
        Repository repository = repository(10L, 20L);
        User user = user(30L);
        Member member = member(40L);
        when(repositoryRepository.findById(10L)).thenReturn(Optional.of(repository));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(user);
        when(memberRepository.findByUserUserIdAndCompanyCompanyId(30L, 20L))
                .thenReturn(Optional.of(member));
        when(repoAssignmentRepository.existsByMemberMemberIdAndRepositoryRepositoryId(40L, 10L))
                .thenReturn(true);

        assertEquals(repository, service.requireRepositoryWriteAccess(10L));
    }

    @Test
    void unassignedCompanyMemberIsDenied() {
        Repository repository = repository(10L, 20L);
        User user = user(30L);
        Member member = member(40L);
        when(repositoryRepository.findById(10L)).thenReturn(Optional.of(repository));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(user);
        when(memberRepository.findByUserUserIdAndCompanyCompanyId(30L, 20L))
                .thenReturn(Optional.of(member));

        assertThrows(AccessDeniedException.class, () -> service.requireRepositoryReadAccess(10L));
    }

    @Test
    void userOutsideRepositoryCompanyIsDenied() {
        Repository repository = repository(10L, 20L);
        when(repositoryRepository.findById(10L)).thenReturn(Optional.of(repository));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(user(30L));
        when(memberRepository.findByUserUserIdAndCompanyCompanyId(30L, 20L))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.requireRepositoryReadAccess(10L));

        verifyNoInteractions(repoAssignmentRepository);
    }

    @Test
    void missingRepositoryReturnsNotFoundBeforeAuthorization() {
        when(repositoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.requireRepositoryReadAccess(404L));

        verifyNoInteractions(auth0UserService, superAdminRepository, memberRepository, repoAssignmentRepository);
    }

    @Test
    void systemAdminHasGlobalRepositoryAndAnalysisReadAccess() {
        authenticateSystemAdmin();
        Repository repository = repository(10L, 20L);
        Analysis_Job analysis = new Analysis_Job();
        analysis.setAnalysisId(50L);
        analysis.setRepository(repository);
        when(repositoryRepository.findById(10L)).thenReturn(Optional.of(repository));
        when(analysisJobRepository.findById(50L)).thenReturn(Optional.of(analysis));

        assertEquals(repository, service.requireRepositoryReadAccess(10L));
        assertEquals(analysis, service.requireAnalysisReadAccess(50L));

        verifyNoInteractions(auth0UserService, superAdminRepository, memberRepository, repoAssignmentRepository);
    }

    @Test
    void systemAdminDoesNotReceiveGlobalRepositoryWriteAccess() {
        authenticateSystemAdmin();
        Repository repository = repository(10L, 20L);
        when(repositoryRepository.findById(10L)).thenReturn(Optional.of(repository));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(user(30L));
        when(memberRepository.findByUserUserIdAndCompanyCompanyId(30L, 20L))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.requireRepositoryWriteAccess(10L));

        verify(repoAssignmentRepository, never())
                .existsByMemberMemberIdAndRepositoryRepositoryId(40L, 10L);
    }

    @Test
    void missingAnalysisReturnsNotFoundBeforeAuthorization() {
        when(analysisJobRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.requireAnalysisReadAccess(404L));

        verifyNoInteractions(auth0UserService, superAdminRepository, memberRepository, repoAssignmentRepository);
    }

    private static void authenticateSystemAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "password", "ROLE_SYSTEM_ADMIN")
        );
    }

    private static Repository repository(Long repositoryId, Long companyId) {
        Company company = new Company();
        company.setCompanyId(companyId);
        Repository repository = new Repository();
        repository.setRepositoryId(repositoryId);
        repository.setCompany(company);
        return repository;
    }

    private static User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        return user;
    }

    private static Member member(Long memberId) {
        Member member = new Member();
        member.setMemberId(memberId);
        return member;
    }
}
