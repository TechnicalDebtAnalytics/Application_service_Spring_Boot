package com.debtlens.backend.integration.github;

import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.github.dto.GithubAppInfoResponse;
import com.debtlens.backend.integration.github.dto.GithubMemberResponse;
import com.debtlens.backend.integration.github.dto.GithubMemberValidationResponse;
import com.debtlens.backend.integration.github.dto.GithubOrgResponse;
import com.debtlens.backend.integration.github.dto.GithubRepoResponse;
import com.debtlens.backend.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GithubService {

    private final GithubClient githubClient;
    private final GithubAppTokenService githubAppTokenService;
    private final UserRepository userRepository;

    public GithubService(GithubClient githubClient, GithubAppTokenService githubAppTokenService, UserRepository userRepository) {
        this.githubClient = githubClient;
        this.githubAppTokenService = githubAppTokenService;
        this.userRepository = userRepository;
    }

    public GithubAppInfoResponse getAppInfo() {
        boolean configured = githubAppTokenService.isConfigured();
        String slug = githubAppTokenService.getAppSlug();
        String installUrl = githubAppTokenService.getInstallUrl();
        return new GithubAppInfoResponse(configured, slug, installUrl);
    }

    /**
     * Get organization details from GitHub.
     */
    public GithubOrgResponse getOrganization(String orgName) {
        return getOrganization(orgName, null);
    }

    public GithubOrgResponse getOrganization(String orgName, Long installationId) {
        validateName(orgName, "Organization name");
        return githubClient.getOrganization(orgName.trim(), installationId);
    }

    /**
     * Get organization repositories for repository selection.
     */
    @Cacheable(value = "github-repos", key = "#orgName.trim().toLowerCase() + '-' + (#installationId != null ? #installationId : 'default')")
    public List<GithubRepoResponse> getRepositories(String orgName, Long installationId) {
        validateName(orgName, "Organization name");
        return githubClient.getOrganizationRepositories(orgName.trim(), installationId);
    }

    public List<GithubRepoResponse> getRepositories(String orgName) {
        return getRepositories(orgName, null);
    }

    /**
     * Get public members of an organization.
     */
    @Cacheable(value = "github-members", key = "#orgName.trim().toLowerCase() + '-' + (#installationId != null ? #installationId : 'default')")
    public List<GithubMemberResponse> getMembers(String orgName, Long installationId) {
        validateName(orgName, "Organization name");
        return githubClient.getOrganizationMembers(orgName.trim(), installationId);
    }

    public List<GithubMemberResponse> getMembers(String orgName) {
        return getMembers(orgName, null);
    }

    /**
     * Get contributors for a specific repository.
     */
    @Cacheable(value = "github-contributors", key = "#owner.trim().toLowerCase() + '/' + #repo.trim().toLowerCase() + '-' + (#installationId != null ? #installationId : 'default')")
    public List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> getContributors(String owner, String repo, Long installationId) {
        validateName(owner, "Repository owner / organization");
        validateName(repo, "Repository name");
        return githubClient.getRepoContributors(owner.trim(), repo.trim(), installationId);
    }

    public List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> getContributors(String owner, String repo) {
        return getContributors(owner, repo, null);
    }

    public GithubMemberValidationResponse validateUserMembership(String orgName, String username) {
        return validateUserMembership(orgName, username, null);
    }

    public GithubMemberValidationResponse validateUserMembership(String orgName, String username, Long installationId) {
        validateName(orgName, "Organization name");
        validateName(username, "GitHub username");

        String trimmedOrg = orgName.trim();
        String trimmedUser = username.trim();

        // 1. Check direct public membership endpoint (GET /orgs/{org}/public_members/{username})
        boolean isPublic = githubClient.isPublicMember(trimmedOrg, trimmedUser, installationId);
        if (isPublic) {
            return new GithubMemberValidationResponse(
                    trimmedOrg,
                    trimmedUser,
                    true,
                    "User '" + trimmedUser + "' is a verified member of '" + trimmedOrg + "'."
            );
        }

        // 2. Fallback: check against fetched member logins in case of casing differences
        List<GithubMemberResponse> members = githubClient.getOrganizationMembers(trimmedOrg, installationId);
        boolean matchedMember = members.stream()
                .anyMatch(m -> m.login() != null && m.login().equalsIgnoreCase(trimmedUser));

        if (matchedMember) {
            return new GithubMemberValidationResponse(
                    trimmedOrg,
                    trimmedUser,
                    true,
                    "User '" + trimmedUser + "' is a verified member of '" + trimmedOrg + "'."
            );
        }

        // 3. Fallback: check repository contributors across the organization
        List<GithubRepoResponse> repos = githubClient.getOrganizationRepositories(trimmedOrg, installationId);
        for (GithubRepoResponse repo : repos) {
            if (repo.name() != null) {
                List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> contribs =
                        githubClient.getRepoContributors(trimmedOrg, repo.name(), installationId);
                boolean isContrib = contribs.stream()
                        .anyMatch(c -> c.login() != null && c.login().equalsIgnoreCase(trimmedUser));
                if (isContrib) {
                    return new GithubMemberValidationResponse(
                            trimmedOrg,
                            trimmedUser,
                            true,
                            "User '" + trimmedUser + "' is a verified contributor of repository '" + repo.name() + "' in '" + trimmedOrg + "'."
                    );
                }
            }
        }

        return new GithubMemberValidationResponse(
                trimmedOrg,
                trimmedUser,
                false,
                "User '" + trimmedUser + "' was not found in the public member list or contributors of '" + trimmedOrg +
                        "'. If you are an org member, please visit https://github.com/orgs/" + trimmedOrg +
                        "/people, find your name, and set your membership to 'Public'."
        );
    }

    /**
     * Validate organization membership by looking up the user's GitHub username from database using their Auth0 User ID.
     */
    public GithubMemberValidationResponse validateUserMembershipByAuth0UserId(String orgName, String auth0UserId) {
        return validateUserMembershipByAuth0UserId(orgName, auth0UserId, null);
    }

    public GithubMemberValidationResponse validateUserMembershipByAuth0UserId(String orgName, String auth0UserId, Long installationId) {
        validateName(orgName, "Organization name");
        validateName(auth0UserId, "Auth0 User ID");

        String trimmedAuth0Id = auth0UserId.trim();
        User user = userRepository.findByAuth0UserId(trimmedAuth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for Auth0 User ID: " + trimmedAuth0Id));

        String githubUsername = user.getGithubUsername();
        if (githubUsername == null || githubUsername.isBlank()) {
            throw new BadRequestException("No GitHub username registered for user with Auth0 ID: " + trimmedAuth0Id);
        }

        return validateUserMembership(orgName, githubUsername, installationId);
    }

    private void validateName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
    }
}