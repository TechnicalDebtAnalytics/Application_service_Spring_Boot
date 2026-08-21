package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.request.InvitationRequestDTO;
import com.debtlens.backend.dto.response.InvitationResponseDTO;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Invitation;
import com.debtlens.backend.entity.InvitationStatus;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Repo_Assignment;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.mapper.InvitationMapper;
import com.debtlens.backend.repository.InvitationRepository;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.Repo_AssignmentRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import com.debtlens.backend.security.Auth0UserService;
import com.debtlens.backend.service.EmailService;
import com.debtlens.backend.service.InvitationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final RepositoryRepository repositoryRepository;
    private final Super_AdminRepository superAdminRepository;
    private final MemberRepository memberRepository;
    private final Repo_AssignmentRepository repoAssignmentRepository;
    private final Auth0UserService auth0UserService;
    private final EmailService emailService;
    private final InvitationMapper invitationMapper;

    @Value("${app.invitation.expiration-days:7}")
    private int expirationDays = 7;

    public InvitationServiceImpl(
            InvitationRepository invitationRepository,
            RepositoryRepository repositoryRepository,
            Super_AdminRepository superAdminRepository,
            MemberRepository memberRepository,
            Repo_AssignmentRepository repoAssignmentRepository,
            Auth0UserService auth0UserService,
            EmailService emailService,
            InvitationMapper invitationMapper
    ) {
        this.invitationRepository = invitationRepository;
        this.repositoryRepository = repositoryRepository;
        this.superAdminRepository = superAdminRepository;
        this.memberRepository = memberRepository;
        this.repoAssignmentRepository = repoAssignmentRepository;
        this.auth0UserService = auth0UserService;
        this.emailService = emailService;
        this.invitationMapper = invitationMapper;
    }

    /**
     * Creates and dispatches contributor invitations for a specific repository.
     */
    @Override
    @Transactional
    public List<InvitationResponseDTO> sendInvitations(InvitationRequestDTO request) {
        if (request.contributors() == null || request.contributors().isEmpty()) {
            throw new BadRequestException("At least one contributor invitation must be provided");
        }

        // 1. Fetch repository
        Repository repository = repositoryRepository.findById(request.repositoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with ID: " + request.repositoryId()));

        Company company = repository.getCompany();
        if (company == null) {
            throw new BadRequestException("Repository is not linked to any company");
        }

        // 2. Validate current user is Super Admin for this company
        User currentUser = auth0UserService.getAuthenticatedUser();
        Super_Admin superAdmin = superAdminRepository
                .findByUserUserIdAndCompanyCompanyId(currentUser.getUserId(), company.getCompanyId())
                .orElseThrow(() -> new BadRequestException("Access denied: You are not a Super Admin for company: " + company.getCompanyName()));

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(expirationDays);
        List<InvitationResponseDTO> createdInvitations = new ArrayList<>();

        // 3. Process each contributor invitation
        for (InvitationRequestDTO.ContributorInviteDTO invite : request.contributors()) {
            String email = invite.email().trim().toLowerCase();
            String githubUsername = invite.githubUsername() != null ? invite.githubUsername().trim() : null;

            if (invitationRepository.existsByEmailAndRepositoryRepositoryIdAndStatus(email, repository.getRepositoryId(), InvitationStatus.PENDING)) {
                throw new BadRequestException("A pending invitation already exists for email: " + email);
            }

            Invitation invitation = new Invitation();
            invitation.setEmail(email);
            invitation.setGithubUsername(githubUsername);
            invitation.setRepository(repository);
            invitation.setSuperAdmin(superAdmin);
            invitation.setToken(UUID.randomUUID().toString());
            invitation.setStatus(InvitationStatus.PENDING);
            invitation.setExpiresAt(expiresAt);
            invitation.setCreatedAt(LocalDateTime.now());

            Invitation savedInvitation = invitationRepository.save(invitation);

            // 4. Send branded HTML invitation email
            emailService.sendInvitationEmail(
                    savedInvitation.getEmail(),
                    savedInvitation.getGithubUsername(),
                    company.getCompanyName(),
                    repository.getRepositoryName(),
                    savedInvitation.getToken(),
                    savedInvitation.getExpiresAt()
            );

            createdInvitations.add(invitationMapper.toDTO(savedInvitation));
        }

        return createdInvitations;
    }

    /**
     * Retrieves all invitations for a given repository.
     */
    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponseDTO> getInvitationsByRepository(Long repositoryId) {
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with ID: " + repositoryId));

        User currentUser = auth0UserService.getAuthenticatedUser();
        Company company = repository.getCompany();

        if (company != null && !superAdminRepository.existsByUserUserIdAndCompanyCompanyId(currentUser.getUserId(), company.getCompanyId())) {
            throw new BadRequestException("Access denied: You are not an admin for this repository");
        }

        return invitationRepository.findByRepositoryRepositoryId(repositoryId)
                .stream()
                .map(invitationMapper::toDTO)
                .toList();
    }

    /**
     * Retrieves all invitations for all repositories under a given company.
     */
    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponseDTO> getInvitationsByCompany(Long companyId) {
        User currentUser = auth0UserService.getAuthenticatedUser();
        if (!superAdminRepository.existsByUserUserIdAndCompanyCompanyId(currentUser.getUserId(), companyId)) {
            throw new BadRequestException("Access denied: You are not an admin for this company");
        }

        return invitationRepository.findByRepositoryCompanyCompanyId(companyId)
                .stream()
                .map(invitationMapper::toDTO)
                .toList();
    }

    /**
     * Retrieves all pending invitations addressed to the currently authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponseDTO> getMyPendingInvitations() {
        User currentUser = auth0UserService.getAuthenticatedUser();
        String githubUsername = currentUser.getGithubUsername() != null ? currentUser.getGithubUsername().trim() : "";
        String email = currentUser.getEmail() != null ? currentUser.getEmail().trim() : "";

        return invitationRepository.findPendingForUser(githubUsername, email)
                .stream()
                .map(invitationMapper::toDTO)
                .toList();
    }

    /**
     * Accepts a pending invitation, creating Member and RepoAssignment records.
     */
    @Override
    @Transactional
    public InvitationResponseDTO acceptInvitation(Long invitationId) {
        User currentUser = auth0UserService.getAuthenticatedUser();
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with ID: " + invitationId));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("This invitation is already " + invitation.getStatus());
        }

        // Check if expired
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BadRequestException("This invitation has expired");
        }

        // Validate recipient match
        boolean matchesUser = (invitation.getGithubUsername() != null && invitation.getGithubUsername().equalsIgnoreCase(currentUser.getGithubUsername()))
                || (invitation.getEmail() != null && invitation.getEmail().equalsIgnoreCase(currentUser.getEmail()));

        if (!matchesUser) {
            throw new BadRequestException("This invitation was not addressed to your GitHub username or email");
        }

        // 1. Mark invitation ACCEPTED
        invitation.setStatus(InvitationStatus.ACCEPTED);
        Invitation savedInvitation = invitationRepository.save(invitation);

        // 2. Ensure Member record exists for this User and Company
        Repository repository = invitation.getRepository();
        Company company = repository.getCompany();

        Member member = memberRepository.findByUserUserIdAndCompanyCompanyId(currentUser.getUserId(), company.getCompanyId())
                .orElseGet(() -> {
                    Member newMember = new Member();
                    newMember.setUser(currentUser);
                    newMember.setCompany(company);
                    newMember.setAssignedBy(invitation.getSuperAdmin() != null ? invitation.getSuperAdmin().getUser() : currentUser);
                    return memberRepository.save(newMember);
                });

        // 3. Ensure RepoAssignment exists for this Member and Repository
        if (!repoAssignmentRepository.existsByMemberMemberIdAndRepositoryRepositoryId(member.getMemberId(), repository.getRepositoryId())) {
            Repo_Assignment assignment = new Repo_Assignment();
            assignment.setId(new com.debtlens.backend.entity.RepoAssignmentId(member.getMemberId(), repository.getRepositoryId()));
            assignment.setMember(member);
            assignment.setRepository(repository);
            repoAssignmentRepository.save(assignment);
        }

        return invitationMapper.toDTO(savedInvitation);
    }

    /**
     * Rejects a pending invitation.
     */
    @Override
    @Transactional
    public InvitationResponseDTO rejectInvitation(Long invitationId) {
        User currentUser = auth0UserService.getAuthenticatedUser();
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with ID: " + invitationId));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("This invitation is already " + invitation.getStatus());
        }

        // Validate recipient match
        boolean matchesUser = (invitation.getGithubUsername() != null && invitation.getGithubUsername().equalsIgnoreCase(currentUser.getGithubUsername()))
                || (invitation.getEmail() != null && invitation.getEmail().equalsIgnoreCase(currentUser.getEmail()));

        if (!matchesUser) {
            throw new BadRequestException("This invitation was not addressed to your GitHub username or email");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        Invitation savedInvitation = invitationRepository.save(invitation);

        return invitationMapper.toDTO(savedInvitation);
    }
}