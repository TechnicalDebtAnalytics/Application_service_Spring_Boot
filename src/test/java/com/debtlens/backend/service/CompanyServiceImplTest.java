package com.debtlens.backend.service;

import com.debtlens.backend.dto.request.CompanyRequestDTO;
import com.debtlens.backend.dto.request.SelectedRepoDTO;
import com.debtlens.backend.dto.response.CompanyAvailableRepoDTO;
import com.debtlens.backend.dto.response.CompanyResponseDTO;
import com.debtlens.backend.dto.response.RepositoryResponseDTO;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Repo_Assignment;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.github.GithubService;
import com.debtlens.backend.integration.github.dto.GithubMemberValidationResponse;
import com.debtlens.backend.integration.github.dto.GithubRepoResponse;
import com.debtlens.backend.mapper.CompanyMapper;
import com.debtlens.backend.mapper.RepositoryMapper;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.Repo_AssignmentRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import com.debtlens.backend.security.Auth0UserService;
import com.debtlens.backend.security.CompanyAccessService;
import com.debtlens.backend.service.impl.CompanyServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock CompanyRepository companyRepository;
    @Mock RepositoryRepository repositoryRepository;
    @Mock Super_AdminRepository superAdminRepository;
    @Mock MemberRepository memberRepository;
    @Mock Repo_AssignmentRepository repoAssignmentRepository;
    @Mock com.debtlens.backend.repository.Analysis_JobRepository analysisJobRepository;
    @Mock com.debtlens.backend.repository.InvitationRepository invitationRepository;
    @Mock Auth0UserService auth0UserService;
    @Mock CompanyAccessService companyAccessService;
    @Mock GithubService githubService;
    @Mock CompanyMapper companyMapper;
    @Mock RepositoryMapper repositoryMapper;

    private CompanyServiceImpl service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new CompanyServiceImpl(companyRepository, repositoryRepository, superAdminRepository,
                memberRepository, repoAssignmentRepository, analysisJobRepository, invitationRepository,
                auth0UserService, companyAccessService, githubService, companyMapper, repositoryMapper);
        owner = user(1L, "owner-gh");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCompany_shouldValidateOrganizationPersistRepositoriesAndCreateSuperAdmin() {
        CompanyResponseDTO expected = org.mockito.Mockito.mock(CompanyResponseDTO.class);
        when(auth0UserService.getAuthenticatedUser()).thenReturn(owner);
        when(githubService.validateUserMembership("acme", "owner-gh"))
                .thenReturn(new GithubMemberValidationResponse("acme", "owner-gh", true, "member"));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.setCompanyId(10L);
            return company;
        });
        when(companyMapper.toDTO(any(Company.class))).thenReturn(expected);
        CompanyRequestDTO request = new CompanyRequestDTO(
                "  Acme Corp  ",
                "https://github.com/acme/?tab=repositories",
                List.of(repo(101L, " backend ", " https://github.com/acme/backend ", null))
        );

        CompanyResponseDTO actual = service.createCompany(request);

        assertSame(expected, actual);
        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        Company saved = companyCaptor.getValue();
        assertEquals("Acme Corp", saved.getCompanyName());
        assertEquals("https://github.com/acme", saved.getGithubOrganizationUrl());
        assertSame(owner, saved.getCreatedBy());
        assertEquals(1, saved.getRepositories().size());
        assertEquals("backend", saved.getRepositories().get(0).getRepositoryName());
        assertEquals("main", saved.getRepositories().get(0).getDefaultBranch());
        assertSame(saved, saved.getRepositories().get(0).getCompany());
        ArgumentCaptor<Super_Admin> adminCaptor = ArgumentCaptor.forClass(Super_Admin.class);
        verify(superAdminRepository).save(adminCaptor.capture());
        assertSame(owner, adminCaptor.getValue().getUser());
        assertSame(saved, adminCaptor.getValue().getCompany());
    }

    @Test
    void createCompany_shouldRejectInvalidOrganization() {
        when(auth0UserService.getAuthenticatedUser()).thenReturn(owner);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.createCompany(new CompanyRequestDTO("Acme", "  ", List.of(repo(1L, "r", "url", "main")))));

        assertEquals("A valid GitHub organization name or URL is required", exception.getMessage());
        verifyNoInteractions(githubService, companyRepository, superAdminRepository);
    }

    @Test
    void createCompany_shouldRejectNonMemberAndDuplicateOrganization() {
        when(auth0UserService.getAuthenticatedUser()).thenReturn(owner);
        when(githubService.validateUserMembership("acme", "owner-gh"))
                .thenReturn(new GithubMemberValidationResponse("acme", "owner-gh", false, "not a member"));
        CompanyRequestDTO request = new CompanyRequestDTO("Acme", "acme", List.of(repo(1L, "r", "url", "main")));

        assertEquals("not a member", assertThrows(BadRequestException.class, () -> service.createCompany(request)).getMessage());

        when(githubService.validateUserMembership("acme", "owner-gh"))
                .thenReturn(new GithubMemberValidationResponse("acme", "owner-gh", true, "member"));
        when(companyRepository.existsByGithubOrganizationUrl("https://github.com/acme")).thenReturn(true);
        assertEquals("A company for GitHub organization 'acme' already exists",
                assertThrows(BadRequestException.class, () -> service.createCompany(request)).getMessage());
        verify(companyRepository, never()).save(any());
    }

    @Test
    void createCompany_shouldRejectEmptyRepositorySelection() {
        when(auth0UserService.getAuthenticatedUser()).thenReturn(owner);
        when(githubService.validateUserMembership("acme", "owner-gh"))
                .thenReturn(new GithubMemberValidationResponse("acme", "owner-gh", true, "member"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.createCompany(new CompanyRequestDTO("Acme", "acme", List.of())));

        assertEquals("Please select at least one repository to add to your company", exception.getMessage());
        verify(companyRepository, never()).save(any());
    }

    @Test
    void getAdminCompaniesAndCompanyById_shouldMapRepositoryResults() {
        Company first = company(10L, owner);
        Company second = company(11L, owner);
        CompanyResponseDTO firstDto = org.mockito.Mockito.mock(CompanyResponseDTO.class);
        CompanyResponseDTO secondDto = org.mockito.Mockito.mock(CompanyResponseDTO.class);
        when(auth0UserService.getAuthenticatedUser()).thenReturn(owner);
        Super_Admin firstAdmin = superAdmin(owner, first);
        Super_Admin secondAdmin = superAdmin(owner, second);
        when(superAdminRepository.findByUserUserId(1L)).thenReturn(List.of(firstAdmin, secondAdmin));
        when(companyMapper.toDTO(first)).thenReturn(firstDto);
        when(companyMapper.toDTO(second)).thenReturn(secondDto);
        when(companyAccessService.requireCompanyAccess(10L)).thenReturn(first);

        assertEquals(List.of(firstDto, secondDto), service.getMyAdminCompanies());
        assertSame(firstDto, service.getCompanyById(10L));
    }

    @Test
    void getCompanyById_shouldFailWhenMissing() {
        when(companyAccessService.requireCompanyAccess(404L))
                .thenThrow(new ResourceNotFoundException("Company not found with ID: 404"));

        assertEquals("Company not found with ID: 404",
                assertThrows(ResourceNotFoundException.class, () -> service.getCompanyById(404L)).getMessage());
    }

    @Test
    void addRepositories_shouldAddOnlyNewRepositories() {
        Company company = company(10L, owner);
        company.addRepository(repository("101", 201L));
        CompanyResponseDTO expected = org.mockito.Mockito.mock(CompanyResponseDTO.class);
        when(companyAccessService.requireSuperAdminAccess(10L)).thenReturn(company);
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toDTO(company)).thenReturn(expected);

        CompanyResponseDTO actual = service.addRepositoriesToCompany(10L, List.of(
                repo(101L, "duplicate", "url-1", "main"),
                repo(102L, " new-repo ", " new-url ", null)
        ));

        assertSame(expected, actual);
        assertEquals(2, company.getRepositories().size());
        assertEquals("102", company.getRepositories().get(1).getGithubRepositoryId());
        assertEquals("new-repo", company.getRepositories().get(1).getRepositoryName());
        assertEquals("main", company.getRepositories().get(1).getDefaultBranch());
    }

    @Test
    void addRepositories_shouldValidateInputEntityAuthorizationAndDuplicates() {
        assertEquals("Please select at least one repository to add",
                assertThrows(BadRequestException.class, () -> service.addRepositoriesToCompany(10L, List.of())).getMessage());

        when(companyAccessService.requireSuperAdminAccess(404L))
                .thenThrow(new ResourceNotFoundException("Company not found with ID: 404"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.addRepositoriesToCompany(404L, List.of(repo(1L, "r", "u", "main"))));

        Company company = company(10L, owner);
        when(companyAccessService.requireSuperAdminAccess(10L))
                .thenThrow(new AccessDeniedException("Access denied"))
                .thenReturn(company);
        assertThrows(AccessDeniedException.class,
                () -> service.addRepositoriesToCompany(10L, List.of(repo(1L, "r", "u", "main"))));

        company.addRepository(repository("1", 100L));
        assertEquals("All selected repositories are already added to this company",
                assertThrows(BadRequestException.class,
                        () -> service.addRepositoriesToCompany(10L, List.of(repo(1L, "r", "u", "main")))).getMessage());
    }

    @Test
    void getAvailableRepositories_shouldFlagExistingGithubRepositories() {
        Company company = company(10L, owner);
        company.setGithubOrganizationUrl("https://github.com/acme");
        company.addRepository(repository("101", 201L));
        when(companyAccessService.requireCompanyAccess(10L)).thenReturn(company);
        when(githubService.getRepositories("acme")).thenReturn(List.of(
                githubRepo(101L, "existing"), githubRepo(102L, "new")
        ));

        List<CompanyAvailableRepoDTO> result = service.getAvailableRepositoriesForCompany(10L);

        assertEquals(List.of(true, false), result.stream().map(CompanyAvailableRepoDTO::alreadyAdded).toList());
    }

    @Test
    void getCompanyRepositories_shouldReturnAllForSystemAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "password", "ROLE_SYSTEM_ADMIN"));
        Repository repository = repository("101", 201L);
        RepositoryResponseDTO dto = org.mockito.Mockito.mock(RepositoryResponseDTO.class);
        when(repositoryRepository.findByCompanyCompanyId(10L)).thenReturn(List.of(repository));
        when(repositoryMapper.toDTO(repository)).thenReturn(dto);

        assertEquals(List.of(dto), service.getCompanyRepositories(10L));
        verifyNoInteractions(auth0UserService);
    }

    @Test
    void getCompanyRepositories_shouldReturnAssignmentsForMemberAndRejectOutsider() {
        Member member = new Member();
        member.setMemberId(20L);
        Repository repository = repository("101", 201L);
        Repo_Assignment assignment = new Repo_Assignment();
        assignment.setRepository(repository);
        RepositoryResponseDTO dto = org.mockito.Mockito.mock(RepositoryResponseDTO.class);
        when(auth0UserService.getAuthenticatedUser()).thenReturn(owner);
        when(memberRepository.findByUserUserIdAndCompanyCompanyId(1L, 10L)).thenReturn(Optional.of(member));
        when(repoAssignmentRepository.findByMemberMemberId(20L)).thenReturn(List.of(assignment));
        when(repositoryMapper.toDTO(repository)).thenReturn(dto);

        assertEquals(List.of(dto), service.getCompanyRepositories(10L));

        when(memberRepository.findByUserUserIdAndCompanyCompanyId(1L, 11L)).thenReturn(Optional.empty());
        assertEquals("Access denied: You are not an authorized member or admin of this company",
                assertThrows(AccessDeniedException.class, () -> service.getCompanyRepositories(11L)).getMessage());
    }

    @Test
    void getMyMemberCompanies_shouldMapOnlyAssignedRepositories() {
        Company company = company(10L, owner);
        Member member = new Member();
        member.setMemberId(20L);
        member.setCompany(company);
        Repo_Assignment assignment = new Repo_Assignment();
        Repository repository = repository("101", 201L);
        assignment.setRepository(repository);
        RepositoryResponseDTO repoDto = org.mockito.Mockito.mock(RepositoryResponseDTO.class);
        CompanyResponseDTO companyDto = org.mockito.Mockito.mock(CompanyResponseDTO.class);
        when(auth0UserService.getAuthenticatedUser()).thenReturn(owner);
        when(memberRepository.findByUserUserId(1L)).thenReturn(List.of(member));
        when(repoAssignmentRepository.findByMemberMemberId(20L)).thenReturn(List.of(assignment));
        when(repositoryMapper.toDTO(repository)).thenReturn(repoDto);
        when(companyMapper.toDTO(company, List.of(repoDto))).thenReturn(companyDto);

        assertEquals(List.of(companyDto), service.getMyMemberCompanies());
    }

    @Test
    void removeRepositoryFromCompany_shouldDeleteJobsAssignmentsInvitationsAndRepository() {
        Company comp = company(10L, owner);
        Repository repo = repository("101", 201L);
        repo.setCompany(comp);
        comp.addRepository(repo);

        when(companyAccessService.requireSuperAdminAccess(10L)).thenReturn(comp);
        when(repositoryRepository.findById(201L)).thenReturn(Optional.of(repo));

        service.removeRepositoryFromCompany(10L, 201L);

        verify(analysisJobRepository).findByRepositoryRepositoryIdOrderByStartedAtDesc(201L);
        verify(repoAssignmentRepository).findByRepositoryRepositoryId(201L);
        verify(invitationRepository).findByRepositoryRepositoryId(201L);
        verify(repositoryRepository).delete(repo);
    }

    @Test
    void removeMemberFromCompany_shouldDeleteAssignmentsAndMember() {
        Company comp = company(10L, owner);
        Member member = new Member();
        member.setMemberId(50L);
        member.setCompany(comp);

        when(companyAccessService.requireSuperAdminAccess(10L)).thenReturn(comp);
        when(memberRepository.findById(50L)).thenReturn(Optional.of(member));

        service.removeMemberFromCompany(10L, 50L);

        verify(repoAssignmentRepository).findByMemberMemberId(50L);
        verify(memberRepository).delete(member);
    }

    @Test
    void getCompanyMembers_shouldReturnMappedMemberList() {
        Company comp = company(10L, owner);
        Member member = new Member();
        member.setMemberId(50L);
        member.setCompany(comp);
        member.setUser(owner);

        when(companyAccessService.requireCompanyAccess(10L)).thenReturn(comp);
        when(memberRepository.findByCompanyCompanyId(10L)).thenReturn(List.of(member));

        var result = service.getCompanyMembers(10L);

        assertEquals(1, result.size());
        assertEquals(50L, result.get(0).memberId());
        assertEquals("owner-gh", result.get(0).githubUsername());
    }

    @Test
    void removalRejectsAnActiveJobBeforeDeletingAnything() {
        Company comp = company(10L, owner);
        Repository repo = repository("101", 201L);
        repo.setCompany(comp);
        var job = new com.debtlens.backend.entity.Analysis_Job();
        job.setStatus(com.debtlens.backend.entity.AnalysisJobStatus.RUNNING);
        when(companyAccessService.requireSuperAdminAccess(10L)).thenReturn(comp);
        when(repositoryRepository.findById(201L)).thenReturn(Optional.of(repo));
        when(analysisJobRepository.findByRepositoryRepositoryIdOrderByStartedAtDesc(201L)).thenReturn(List.of(job));
        assertThrows(BadRequestException.class, () -> service.removeRepositoryFromCompany(10L, 201L));
        verify(repositoryRepository, never()).delete(any());
        verify(analysisJobRepository, never()).deleteAll(org.mockito.ArgumentMatchers.<Iterable<com.debtlens.backend.entity.Analysis_Job>>any());
        verifyNoInteractions(repoAssignmentRepository, invitationRepository);
    }

    @Test
    void removingAMemberRevokesOnlyTheirPendingCompanyInvitations() {
        Company comp = company(10L, owner);
        Member member = new Member();
        member.setMemberId(50L);
        member.setCompany(comp);
        member.setUser(owner);
        owner.setEmail("owner@example.com");
        var pending = new com.debtlens.backend.entity.Invitation();
        pending.setEmail("OWNER@example.com");
        pending.setStatus(com.debtlens.backend.entity.InvitationStatus.PENDING);
        var accepted = new com.debtlens.backend.entity.Invitation();
        accepted.setGithubUsername("owner-gh");
        accepted.setStatus(com.debtlens.backend.entity.InvitationStatus.ACCEPTED);
        var other = new com.debtlens.backend.entity.Invitation();
        other.setEmail("other@example.com");
        other.setStatus(com.debtlens.backend.entity.InvitationStatus.PENDING);
        when(companyAccessService.requireSuperAdminAccess(10L)).thenReturn(comp);
        when(memberRepository.findById(50L)).thenReturn(Optional.of(member));
        when(invitationRepository.findByRepositoryCompanyCompanyId(10L)).thenReturn(List.of(pending, accepted, other));
        service.removeMemberFromCompany(10L, 50L);
        verify(invitationRepository).delete(pending);
        verify(invitationRepository, never()).delete(accepted);
        verify(invitationRepository, never()).delete(other);
        verify(memberRepository).delete(member);
    }

    private static User user(Long id, String githubUsername) {
        User user = new User();
        user.setUserId(id);
        user.setGithubUsername(githubUsername);
        return user;
    }

    private static Company company(Long id, User creator) {
        Company company = new Company();
        company.setCompanyId(id);
        company.setCompanyName("Acme");
        company.setCreatedBy(creator);
        return company;
    }

    private static Super_Admin superAdmin(User user, Company company) {
        Super_Admin superAdmin = new Super_Admin();
        superAdmin.setUser(user);
        superAdmin.setCompany(company);
        return superAdmin;
    }

    private static Repository repository(String githubId, Long id) {
        Repository repository = new Repository();
        repository.setRepositoryId(id);
        repository.setGithubRepositoryId(githubId);
        return repository;
    }

    private static SelectedRepoDTO repo(Long id, String name, String url, String branch) {
        return new SelectedRepoDTO(id, name, url, branch);
    }

    private static GithubRepoResponse githubRepo(Long id, String name) {
        return new GithubRepoResponse(id, name, "acme/" + name, "url", "main", null,
                false, "Java", 0, 0, null);
    }
}
