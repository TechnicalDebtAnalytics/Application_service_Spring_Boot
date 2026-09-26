package com.debtlens.backend.service;

import com.debtlens.backend.dto.request.InvitationRequestDTO;
import com.debtlens.backend.dto.response.InvitationResponseDTO;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Invitation;
import com.debtlens.backend.entity.InvitationStatus;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.mapper.InvitationMapper;
import com.debtlens.backend.repository.InvitationRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import com.debtlens.backend.security.Auth0UserService;
import com.debtlens.backend.service.impl.InvitationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private Super_AdminRepository superAdminRepository;

    @Mock
    private com.debtlens.backend.repository.MemberRepository memberRepository;

    @Mock
    private com.debtlens.backend.repository.Repo_AssignmentRepository repoAssignmentRepository;

    @Mock
    private Auth0UserService auth0UserService;

    @Mock
    private EmailService emailService;

    private InvitationMapper invitationMapper;
    private InvitationServiceImpl invitationService;

    private User adminUser;
    private Company testCompany;
    private Repository testRepo;
    private Super_Admin testSuperAdmin;

    @BeforeEach
    void setUp() {
        invitationMapper = new InvitationMapper();
        invitationService = new InvitationServiceImpl(
                invitationRepository,
                repositoryRepository,
                superAdminRepository,
                memberRepository,
                repoAssignmentRepository,
                auth0UserService,
                emailService,
                invitationMapper
        );

        adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setEmail("admin@example.com");
        adminUser.setGithubUsername("admin-gh");

        testCompany = new Company();
        testCompany.setCompanyId(10L);
        testCompany.setCompanyName("Acme Corp");

        testRepo = new Repository();
        testRepo.setRepositoryId(100L);
        testRepo.setRepositoryName("backend-repo");
        testRepo.setCompany(testCompany);

        testSuperAdmin = new Super_Admin();
        testSuperAdmin.setSuperAdminId(5L);
        testSuperAdmin.setUser(adminUser);
        testSuperAdmin.setCompany(testCompany);
    }

    @Test
    void sendInvitations_success() {
        when(repositoryRepository.findById(100L)).thenReturn(Optional.of(testRepo));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(adminUser);
        when(superAdminRepository.findByUserUserIdAndCompanyCompanyId(1L, 10L)).thenReturn(Optional.of(testSuperAdmin));
        when(invitationRepository.existsByEmailAndRepositoryRepositoryIdAndStatus("alice@example.com", 100L, InvitationStatus.PENDING))
                .thenReturn(false);

        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> {
            Invitation inv = invocation.getArgument(0);
            inv.setInvitationId(1001L);
            return inv;
        });

        InvitationRequestDTO request = new InvitationRequestDTO(
                100L,
                List.of(new InvitationRequestDTO.ContributorInviteDTO("alice", "alice@example.com"))
        );

        List<InvitationResponseDTO> result = invitationService.sendInvitations(request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("alice@example.com", result.get(0).email());
        assertEquals("alice", result.get(0).githubUsername());
        assertEquals(InvitationStatus.PENDING, result.get(0).status());
        assertNotNull(result.get(0).token());

        verify(emailService, times(1)).sendInvitationEmail(
                eq("alice@example.com"),
                eq("alice"),
                eq("Acme Corp"),
                eq("backend-repo"),
                any(String.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void sendInvitations_unauthorizedNotSuperAdmin() {
        when(repositoryRepository.findById(100L)).thenReturn(Optional.of(testRepo));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(adminUser);
        when(superAdminRepository.findByUserUserIdAndCompanyCompanyId(1L, 10L)).thenReturn(Optional.empty());

        InvitationRequestDTO request = new InvitationRequestDTO(
                100L,
                List.of(new InvitationRequestDTO.ContributorInviteDTO("alice", "alice@example.com"))
        );

        assertThrows(BadRequestException.class, () -> invitationService.sendInvitations(request));
        verify(emailService, never()).sendInvitationEmail(any(), any(), any(), any(), any(), any());
    }

    @Test
    void sendInvitations_duplicatePendingInvite_throwsBadRequest() {
        when(repositoryRepository.findById(100L)).thenReturn(Optional.of(testRepo));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(adminUser);
        when(superAdminRepository.findByUserUserIdAndCompanyCompanyId(1L, 10L)).thenReturn(Optional.of(testSuperAdmin));
        when(invitationRepository.existsByEmailAndRepositoryRepositoryIdAndStatus("alice@example.com", 100L, InvitationStatus.PENDING))
                .thenReturn(true);

        InvitationRequestDTO request = new InvitationRequestDTO(
                100L,
                List.of(new InvitationRequestDTO.ContributorInviteDTO("alice", "alice@example.com"))
        );

        assertThrows(BadRequestException.class, () -> invitationService.sendInvitations(request));
        verify(emailService, never()).sendInvitationEmail(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getInvitationsByRepository_success() {
        when(repositoryRepository.findById(100L)).thenReturn(Optional.of(testRepo));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(adminUser);
        when(superAdminRepository.existsByUserUserIdAndCompanyCompanyId(1L, 10L)).thenReturn(true);

        Invitation inv = new Invitation();
        inv.setInvitationId(1L);
        inv.setEmail("bob@example.com");
        inv.setGithubUsername("bob");
        inv.setRepository(testRepo);
        inv.setSuperAdmin(testSuperAdmin);
        inv.setStatus(InvitationStatus.PENDING);

        when(invitationRepository.findByRepositoryRepositoryId(100L)).thenReturn(List.of(inv));

        List<InvitationResponseDTO> result = invitationService.getInvitationsByRepository(100L);

        assertEquals(1, result.size());
        assertEquals("bob@example.com", result.get(0).email());
        assertEquals("bob", result.get(0).githubUsername());
    }

    @Test
    void acceptInvitation_success() {
        User invitee = new User();
        invitee.setUserId(2L);
        invitee.setGithubUsername("alice");
        invitee.setEmail("alice@example.com");

        Invitation inv = new Invitation();
        inv.setInvitationId(50L);
        inv.setEmail("alice@example.com");
        inv.setGithubUsername("alice");
        inv.setRepository(testRepo);
        inv.setSuperAdmin(testSuperAdmin);
        inv.setStatus(InvitationStatus.PENDING);
        inv.setExpiresAt(LocalDateTime.now().plusDays(5));

        when(auth0UserService.getAuthenticatedUser()).thenReturn(invitee);
        when(invitationRepository.findById(50L)).thenReturn(Optional.of(inv));
        when(memberRepository.findByUserUserIdAndCompanyCompanyId(2L, 10L)).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(i -> {
            Member m = i.getArgument(0);
            m.setMemberId(300L);
            return m;
        });
        when(repoAssignmentRepository.existsByMemberMemberIdAndRepositoryRepositoryId(300L, 100L)).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        InvitationResponseDTO response = invitationService.acceptInvitation(50L);

        assertNotNull(response);
        assertEquals(InvitationStatus.ACCEPTED, response.status());
        verify(memberRepository, times(1)).save(any(Member.class));
        verify(repoAssignmentRepository, times(1)).save(any(com.debtlens.backend.entity.Repo_Assignment.class));
    }

    @Test
    void rejectInvitation_success() {
        User invitee = new User();
        invitee.setUserId(2L);
        invitee.setGithubUsername("alice");
        invitee.setEmail("alice@example.com");

        Invitation inv = new Invitation();
        inv.setInvitationId(50L);
        inv.setEmail("alice@example.com");
        inv.setGithubUsername("alice");
        inv.setRepository(testRepo);
        inv.setSuperAdmin(testSuperAdmin);
        inv.setStatus(InvitationStatus.PENDING);

        when(auth0UserService.getAuthenticatedUser()).thenReturn(invitee);
        when(invitationRepository.findById(50L)).thenReturn(Optional.of(inv));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        InvitationResponseDTO response = invitationService.rejectInvitation(50L);

        assertNotNull(response);
        assertEquals(InvitationStatus.REJECTED, response.status());
    }

    @Test
    void sendInvitations_rejectsEmptyContributorsAndMissingRepository() {
        InvitationRequestDTO empty = new InvitationRequestDTO(100L, List.of());
        assertEquals("At least one contributor invitation must be provided",
                assertThrows(BadRequestException.class, () -> invitationService.sendInvitations(empty)).getMessage());

        when(repositoryRepository.findById(404L)).thenReturn(Optional.empty());
        InvitationRequestDTO missing = new InvitationRequestDTO(404L,
                List.of(new InvitationRequestDTO.ContributorInviteDTO("alice", "alice@example.com")));
        assertThrows(ResourceNotFoundException.class, () -> invitationService.sendInvitations(missing));
        verifyNoInteractions(emailService);
    }

    @Test
    void sendInvitations_rejectsRepositoryWithoutCompany() {
        testRepo.setCompany(null);
        when(repositoryRepository.findById(100L)).thenReturn(Optional.of(testRepo));
        InvitationRequestDTO request = new InvitationRequestDTO(100L,
                List.of(new InvitationRequestDTO.ContributorInviteDTO("alice", "alice@example.com")));

        assertEquals("Repository is not linked to any company",
                assertThrows(BadRequestException.class, () -> invitationService.sendInvitations(request)).getMessage());
    }

    @Test
    void getInvitationsByRepository_rejectsMissingRepositoryAndUnauthorizedUser() {
        when(repositoryRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invitationService.getInvitationsByRepository(404L));

        when(repositoryRepository.findById(100L)).thenReturn(Optional.of(testRepo));
        when(auth0UserService.getAuthenticatedUser()).thenReturn(adminUser);
        when(superAdminRepository.existsByUserUserIdAndCompanyCompanyId(1L, 10L)).thenReturn(false);
        assertThrows(BadRequestException.class, () -> invitationService.getInvitationsByRepository(100L));
        verify(invitationRepository, never()).findByRepositoryRepositoryId(100L);
    }

    @Test
    void companyAndCurrentUserInvitationQueries_validateAuthorizationAndIdentity() {
        Invitation invitation = invitationFor(new User(), InvitationStatus.PENDING);
        when(auth0UserService.getAuthenticatedUser()).thenReturn(adminUser);
        when(superAdminRepository.existsByUserUserIdAndCompanyCompanyId(1L, 10L)).thenReturn(true);
        when(invitationRepository.findByRepositoryCompanyCompanyId(10L)).thenReturn(List.of(invitation));

        assertEquals(1, invitationService.getInvitationsByCompany(10L).size());

        when(superAdminRepository.existsByUserUserIdAndCompanyCompanyId(1L, 11L)).thenReturn(false);
        assertThrows(BadRequestException.class, () -> invitationService.getInvitationsByCompany(11L));

        adminUser.setGithubUsername("  admin-gh  ");
        adminUser.setEmail("  admin@example.com  ");
        when(invitationRepository.findPendingForUser("admin-gh", "admin@example.com"))
                .thenReturn(List.of(invitation));
        assertEquals(1, invitationService.getMyPendingInvitations().size());
    }

    @Test
    void acceptInvitation_rejectsMissingAlreadyProcessedExpiredAndWrongRecipient() {
        User invitee = invitee();
        when(auth0UserService.getAuthenticatedUser()).thenReturn(invitee);
        when(invitationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invitationService.acceptInvitation(404L));

        Invitation accepted = invitationFor(invitee, InvitationStatus.ACCEPTED);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(accepted));
        assertThrows(BadRequestException.class, () -> invitationService.acceptInvitation(1L));

        Invitation expired = invitationFor(invitee, InvitationStatus.PENDING);
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(invitationRepository.findById(2L)).thenReturn(Optional.of(expired));
        assertEquals("This invitation has expired",
                assertThrows(BadRequestException.class, () -> invitationService.acceptInvitation(2L)).getMessage());
        assertEquals(InvitationStatus.EXPIRED, expired.getStatus());
        verify(invitationRepository).save(expired);

        Invitation wrong = invitationFor(invitee, InvitationStatus.PENDING);
        wrong.setEmail("other@example.com");
        wrong.setGithubUsername("other");
        when(invitationRepository.findById(3L)).thenReturn(Optional.of(wrong));
        assertThrows(BadRequestException.class, () -> invitationService.acceptInvitation(3L));
    }

    @Test
    void acceptInvitation_reusesExistingMemberAndAssignment() {
        User invitee = invitee();
        Invitation invitation = invitationFor(invitee, InvitationStatus.PENDING);
        Member member = new Member();
        member.setMemberId(300L);
        member.setUser(invitee);
        member.setCompany(testCompany);
        when(auth0UserService.getAuthenticatedUser()).thenReturn(invitee);
        when(invitationRepository.findById(50L)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(invitation)).thenReturn(invitation);
        when(memberRepository.findByUserUserIdAndCompanyCompanyId(2L, 10L)).thenReturn(Optional.of(member));
        when(repoAssignmentRepository.existsByMemberMemberIdAndRepositoryRepositoryId(300L, 100L)).thenReturn(true);

        InvitationResponseDTO response = invitationService.acceptInvitation(50L);

        assertEquals(InvitationStatus.ACCEPTED, response.status());
        verify(memberRepository, never()).save(any());
        verify(repoAssignmentRepository, never()).save(any());
    }

    @Test
    void rejectInvitation_rejectsMissingProcessedAndWrongRecipient() {
        User invitee = invitee();
        when(auth0UserService.getAuthenticatedUser()).thenReturn(invitee);
        when(invitationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invitationService.rejectInvitation(404L));

        Invitation processed = invitationFor(invitee, InvitationStatus.REJECTED);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(processed));
        assertThrows(BadRequestException.class, () -> invitationService.rejectInvitation(1L));

        Invitation wrong = invitationFor(invitee, InvitationStatus.PENDING);
        wrong.setEmail("other@example.com");
        wrong.setGithubUsername("other");
        when(invitationRepository.findById(2L)).thenReturn(Optional.of(wrong));
        assertThrows(BadRequestException.class, () -> invitationService.rejectInvitation(2L));
    }

    private User invitee() {
        User invitee = new User();
        invitee.setUserId(2L);
        invitee.setGithubUsername("alice");
        invitee.setEmail("alice@example.com");
        return invitee;
    }

    private Invitation invitationFor(User invitee, InvitationStatus status) {
        Invitation invitation = new Invitation();
        invitation.setInvitationId(50L);
        invitation.setEmail(invitee.getEmail() != null ? invitee.getEmail() : "alice@example.com");
        invitation.setGithubUsername(invitee.getGithubUsername() != null ? invitee.getGithubUsername() : "alice");
        invitation.setRepository(testRepo);
        invitation.setSuperAdmin(testSuperAdmin);
        invitation.setStatus(status);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
        return invitation;
    }
}
