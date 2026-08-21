package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.request.CompanyRequestDTO;
import com.debtlens.backend.dto.request.SelectedRepoDTO;
import com.debtlens.backend.dto.response.CompanyAvailableRepoDTO;
import com.debtlens.backend.dto.response.CompanyResponseDTO;
import com.debtlens.backend.dto.response.RepositoryResponseDTO;
import com.debtlens.backend.entity.Company;
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
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import com.debtlens.backend.security.Auth0UserService;
import com.debtlens.backend.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final RepositoryRepository repositoryRepository;
    private final Super_AdminRepository superAdminRepository;
    private final com.debtlens.backend.repository.MemberRepository memberRepository;
    private final com.debtlens.backend.repository.Repo_AssignmentRepository repoAssignmentRepository;
    private final Auth0UserService auth0UserService;
    private final GithubService githubService;
    private final CompanyMapper companyMapper;
    private final RepositoryMapper repositoryMapper;

    public CompanyServiceImpl(
            CompanyRepository companyRepository,
            RepositoryRepository repositoryRepository,
            Super_AdminRepository superAdminRepository,
            com.debtlens.backend.repository.MemberRepository memberRepository,
            com.debtlens.backend.repository.Repo_AssignmentRepository repoAssignmentRepository,
            Auth0UserService auth0UserService,
            GithubService githubService,
            CompanyMapper companyMapper,
            RepositoryMapper repositoryMapper
    ) {
        this.companyRepository = companyRepository;
        this.repositoryRepository = repositoryRepository;
        this.superAdminRepository = superAdminRepository;
        this.memberRepository = memberRepository;
        this.repoAssignmentRepository = repoAssignmentRepository;
        this.auth0UserService = auth0UserService;
        this.githubService = githubService;
        this.companyMapper = companyMapper;
        this.repositoryMapper = repositoryMapper;
    }

    /**
     * Creates a new company record from a verified GitHub organization.
     * <p>
     * Workflow:
     * 1. Fetches the currently authenticated user.
     * 2. Validates user's membership/contributor access in the target GitHub organization.
     * 3. Ensures no duplicate company exists for the same GitHub organization URL.
     * 4. Persists the Company entity with created_by set to the authenticated user.
     * 5. Maps and attaches the selected GitHub repositories to the company.
     * 6. Automatically registers the creator as a Super Admin for this company.
     *
     * @param request DTO containing the organization name, company name, and selected repositories.
     * @return CompanyResponseDTO containing the created company details and its repositories.
     */
    @Override
    @Transactional
    public CompanyResponseDTO createCompany(CompanyRequestDTO request) {
        // 1. Get authenticated user from security context
        User currentUser = auth0UserService.getAuthenticatedUser();

        // 2. Validate GitHub membership/contributor status of current user
        String orgName = request.githubOrganizationName().trim();
        GithubMemberValidationResponse validation = githubService.validateUserMembership(orgName, currentUser.getGithubUsername());
        if (!validation.isMember()) {
            throw new BadRequestException(validation.message());
        }

        // Check for duplicate company registration for the same GitHub organization
        String orgUrl = "https://github.com/" + orgName;
        if (companyRepository.existsByGithubOrganizationUrl(orgUrl)) {
            throw new BadRequestException("A company for GitHub organization '" + orgName + "' already exists");
        }

        // 3. Create Company entity with created_by = currentUser
        Company company = new Company();
        company.setCompanyName(request.companyName().trim());
        company.setGithubOrganizationUrl(orgUrl);
        company.setCreatedBy(currentUser);

        // 4. Validate and assign selected repositories under the company
        if (request.selectedRepositories() == null || request.selectedRepositories().isEmpty()) {
            throw new BadRequestException("Please select at least one repository to add to your company");
        }

        for (SelectedRepoDTO repoDTO : request.selectedRepositories()) {
            Repository repository = new Repository();
            repository.setGithubRepositoryId(String.valueOf(repoDTO.githubRepositoryId()));
            repository.setRepositoryName(repoDTO.repositoryName().trim());
            repository.setRepositoryUrl(repoDTO.repositoryUrl().trim());
            repository.setDefaultBranch(repoDTO.defaultBranch() != null ? repoDTO.defaultBranch().trim() : "main");
            company.addRepository(repository);
        }

        // 5. Save Company and cascade persist all attached repositories
        Company savedCompany = companyRepository.save(company);

        // 6. Create and link a Super Admin record for this user and company
        Super_Admin superAdmin = new Super_Admin();
        superAdmin.setUser(currentUser);
        superAdmin.setCompany(savedCompany);
        superAdminRepository.save(superAdmin);

        return companyMapper.toDTO(savedCompany);
    }

    /**
     * Retrieves all companies created/managed by the currently authenticated Super Admin.
     *
     * @return List of CompanyResponseDTO for companies created by the current user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponseDTO> getMyAdminCompanies() {
        // 1. Get current authenticated user
        User currentUser = auth0UserService.getAuthenticatedUser();

        // 2. Fetch all companies created by this user
        List<Company> companies = companyRepository.findByCreatedByUserId(currentUser.getUserId());

        // 3. Convert entities to response DTOs
        return companies.stream()
                .map(companyMapper::toDTO)
                .toList();
    }

    /**
     * Retrieves the details of a specific company by its ID.
     *
     * @param companyId Primary key of the company.
     * @return CompanyResponseDTO containing company and repository information.
     * @throws ResourceNotFoundException if company with the given ID does not exist.
     */
    @Override
    @Transactional(readOnly = true)
    public CompanyResponseDTO getCompanyById(Long companyId) {
        // 1. Find company by ID or throw 404 if not found
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        // 2. Map and return response DTO
        return companyMapper.toDTO(company);
    }

    /**
     * Adds additional repositories to an existing company.
     * <p>
     * Ensures only the Super Admin who created the company can add repositories,
     * and filters out any repositories that have already been imported.
     *
     * @param companyId Primary key of the company.
     * @param newRepos  List of new repositories to add.
     * @return Updated CompanyResponseDTO with the new repositories attached.
     * @throws ResourceNotFoundException if company is not found.
     * @throws BadRequestException if access is denied or all repositories are already added.
     */
    @Override
    @Transactional
    public CompanyResponseDTO addRepositoriesToCompany(Long companyId, List<SelectedRepoDTO> newRepos) {
        // 1. Validate request payload
        if (newRepos == null || newRepos.isEmpty()) {
            throw new BadRequestException("Please select at least one repository to add");
        }

        // 2. Authenticate user and fetch the target company
        User currentUser = auth0UserService.getAuthenticatedUser();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        // 3. Check authorization: only the creating Super Admin can modify the company
        if (!company.getCreatedBy().getUserId().equals(currentUser.getUserId())) {
            throw new BadRequestException("Access denied: You are not the Super Admin who created this company");
        }

        // 4. Identify existing repository IDs to prevent duplicates
        Set<String> existingGithubRepoIds = company.getRepositories().stream()
                .map(Repository::getGithubRepositoryId)
                .collect(Collectors.toSet());

        // 5. Append only newly selected repositories
        int addedCount = 0;
        for (SelectedRepoDTO repoDTO : newRepos) {
            String repoIdStr = String.valueOf(repoDTO.githubRepositoryId());
            if (!existingGithubRepoIds.contains(repoIdStr)) {
                Repository repository = new Repository();
                repository.setGithubRepositoryId(repoIdStr);
                repository.setRepositoryName(repoDTO.repositoryName().trim());
                repository.setRepositoryUrl(repoDTO.repositoryUrl().trim());
                repository.setDefaultBranch(repoDTO.defaultBranch() != null ? repoDTO.defaultBranch().trim() : "main");
                company.addRepository(repository);
                existingGithubRepoIds.add(repoIdStr);
                addedCount++;
            }
        }

        if (addedCount == 0) {
            throw new BadRequestException("All selected repositories are already added to this company");
        }

        // 6. Save updated company with new repositories cascaded
        Company updatedCompany = companyRepository.save(company);
        return companyMapper.toDTO(updatedCompany);
    }

    /**
     * Fetches all live repositories from the company's GitHub organization and flags
     * which ones are already added to the company in the database.
     *
     * @param companyId Primary key of the company.
     * @return List of CompanyAvailableRepoDTO indicating repo status and metadata.
     * @throws ResourceNotFoundException if company is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CompanyAvailableRepoDTO> getAvailableRepositoriesForCompany(Long companyId) {
        // 1. Fetch company or throw 404
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        // 2. Extract GitHub organization login name from the organization URL
        String orgUrl = company.getGithubOrganizationUrl();
        String orgName = orgUrl != null && orgUrl.contains("/")
                ? orgUrl.substring(orgUrl.lastIndexOf('/') + 1)
                : orgUrl;

        // 3. Fetch live repositories from GitHub API
        List<GithubRepoResponse> githubRepos = githubService.getRepositories(orgName);

        // 4. Collect existing GitHub repository IDs already saved in database
        Set<String> existingIds = company.getRepositories().stream()
                .map(Repository::getGithubRepositoryId)
                .collect(Collectors.toSet());

        // 5. Map GitHub repositories with 'alreadyAdded' flag
        return githubRepos.stream()
                .map(gr -> new CompanyAvailableRepoDTO(
                        gr.id(),
                        gr.name(),
                        gr.fullName(),
                        gr.htmlUrl(),
                        gr.defaultBranch(),
                        gr.description(),
                        existingIds.contains(String.valueOf(gr.id())),
                        gr.language(),
                        gr.stargazersCount()
                ))
                .toList();
    }

    /**
     * Retrieves repositories for a company. Super Admins receive all company repositories.
     * Members receive only the repositories explicitly assigned to them by the admin.
     *
     * @param companyId Primary key of the company.
     * @return List of RepositoryResponseDTO accessible to the current user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResponseDTO> getCompanyRepositories(Long companyId) {
        User currentUser = auth0UserService.getAuthenticatedUser();

        // 1. Check if user is Super Admin for this company
        boolean isSuperAdmin = superAdminRepository.existsByUserUserIdAndCompanyCompanyId(currentUser.getUserId(), companyId);
        if (isSuperAdmin) {
            List<Repository> repos = repositoryRepository.findByCompanyCompanyId(companyId);
            return repos.stream().map(repositoryMapper::toDTO).toList();
        }

        // 2. Check if user is Member for this company
        var memberOpt = memberRepository.findByUserUserIdAndCompanyCompanyId(currentUser.getUserId(), companyId);
        if (memberOpt.isPresent()) {
            List<com.debtlens.backend.entity.Repo_Assignment> assignments =
                    repoAssignmentRepository.findByMemberMemberId(memberOpt.get().getMemberId());

            return assignments.stream()
                    .map(com.debtlens.backend.entity.Repo_Assignment::getRepository)
                    .map(repositoryMapper::toDTO)
                    .toList();
        }

        // 3. User is neither Super Admin nor Member
        throw new BadRequestException("Access denied: You are not an authorized member or admin of this company");
    }

    /**
     * Retrieves all companies where the authenticated user is a Member,
     * including only the repositories assigned to that member.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponseDTO> getMyMemberCompanies() {
        User currentUser = auth0UserService.getAuthenticatedUser();
        List<com.debtlens.backend.entity.Member> memberships = memberRepository.findByUserUserId(currentUser.getUserId());

        return memberships.stream()
                .map(membership -> {
                    Company company = membership.getCompany();
                    List<com.debtlens.backend.entity.Repo_Assignment> assignments =
                            repoAssignmentRepository.findByMemberMemberId(membership.getMemberId());

                    List<RepositoryResponseDTO> assignedRepoDTOs = assignments.stream()
                            .map(com.debtlens.backend.entity.Repo_Assignment::getRepository)
                            .map(repositoryMapper::toDTO)
                            .toList();

                    return companyMapper.toDTO(company, assignedRepoDTOs);
                })
                .toList();
    }
}