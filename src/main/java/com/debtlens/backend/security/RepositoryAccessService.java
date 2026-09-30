package com.debtlens.backend.security;

import com.debtlens.backend.entity.Analysis_Job;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Repository;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.Analysis_JobRepository;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.Repo_AssignmentRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class RepositoryAccessService {

    private final RepositoryRepository repositoryRepository;
    private final Analysis_JobRepository analysisJobRepository;
    private final Super_AdminRepository superAdminRepository;
    private final MemberRepository memberRepository;
    private final Repo_AssignmentRepository repoAssignmentRepository;
    private final Auth0UserService auth0UserService;

    public RepositoryAccessService(
            RepositoryRepository repositoryRepository,
            Analysis_JobRepository analysisJobRepository,
            Super_AdminRepository superAdminRepository,
            MemberRepository memberRepository,
            Repo_AssignmentRepository repoAssignmentRepository,
            Auth0UserService auth0UserService
    ) {
        this.repositoryRepository = repositoryRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.superAdminRepository = superAdminRepository;
        this.memberRepository = memberRepository;
        this.repoAssignmentRepository = repoAssignmentRepository;
        this.auth0UserService = auth0UserService;
    }

    public Repository requireRepositoryWriteAccess(Long repositoryId) {
        Repository repository = findRepository(repositoryId);
        requireTenantRepositoryAccess(repository);
        return repository;
    }

    public Repository requireRepositoryReadAccess(Long repositoryId) {
        Repository repository = findRepository(repositoryId);
        if (!isSystemAdmin()) {
            requireTenantRepositoryAccess(repository);
        }
        return repository;
    }

    public Analysis_Job requireAnalysisReadAccess(Long analysisId) {
        Analysis_Job analysis = analysisJobRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Analysis job with ID " + analysisId + " not found"
                ));

        if (!isSystemAdmin()) {
            requireTenantRepositoryAccess(analysis.getRepository());
        }
        return analysis;
    }

    private Repository findRepository(Long repositoryId) {
        return repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Repository with ID " + repositoryId + " not found"
                ));
    }

    private void requireTenantRepositoryAccess(Repository repository) {
        User currentUser = auth0UserService.getAuthenticatedUser();
        Long userId = currentUser.getUserId();
        if (repository.getCompany() == null) {
            return;
        }
        Long companyId = repository.getCompany().getCompanyId();

        if (repository.getCompany().getCreatedBy() != null && repository.getCompany().getCreatedBy().getUserId().equals(userId)) {
            return;
        }

        if (superAdminRepository.existsByUserUserIdAndCompanyCompanyId(userId, companyId)) {
            return;
        }

        Member member = memberRepository.findByUserUserIdAndCompanyCompanyId(userId, companyId)
                .orElseThrow(() -> denied(repository.getRepositoryId()));

        if (!repoAssignmentRepository.existsByMemberMemberIdAndRepositoryRepositoryId(
                member.getMemberId(), repository.getRepositoryId())) {
            throw denied(repository.getRepositoryId());
        }
    }

    private boolean isSystemAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SYSTEM_ADMIN".equals(authority.getAuthority()));
    }

    private AccessDeniedException denied(Long repositoryId) {
        return new AccessDeniedException(
                "Access denied: You are not authorized to access repository " + repositoryId
        );
    }
}
