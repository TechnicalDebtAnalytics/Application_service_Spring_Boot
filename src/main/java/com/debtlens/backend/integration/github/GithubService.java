package com.debtlens.backend.integration.github;

import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.github.dto.GithubMemberResponse;
import com.debtlens.backend.integration.github.dto.GithubMemberValidationResponse;
import com.debtlens.backend.integration.github.dto.GithubOrgResponse;
import com.debtlens.backend.integration.github.dto.GithubRepoResponse;
import com.debtlens.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GithubService {

    private final GithubClient githubClient;
    private final UserRepository userRepository;

    public GithubService(GithubClient githubClient, UserRepository userRepository) {
        this.githubClient = githubClient;
        this.userRepository = userRepository;
    }

    /**
     * Get organization details from GitHub.
     */
    public GithubOrgResponse getOrganization(String orgName) {
        validateName(orgName, "Organization name");
        return githubClient.getOrganization(orgName.trim());
    }

    /**
     * Get organization repositories for repository selection.
     */
    public List<GithubRepoResponse> getRepositories(String orgName) {
        validateName(orgName, "Organization name");
        return githubClient.getOrganizationRepositories(orgName.trim());
    }

    /**
     * Get public members of an organization.
     */
    public List<GithubMemberResponse> getMembers(String orgName) {
        validateName(orgName, "Organization name");
        return githubClient.getOrganizationMembers(orgName.trim());
    }

    /**
     * Get contributors for a specific repository.
     */
    public List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> getContributors(String owner, String repo) {
        validateName(owner, "Repository owner / organization");
        validateName(repo, "Repository name");
        return githubClient.getRepoContributors(owner.trim(), repo.trim());
    }

    /**
     * Validate whether a GitHub username belongs to the organization.
     * Uses public membership checking and fallback member scanning.
     */
    public GithubMemberValidationResponse validateUserMembership(String orgName, String username) {
        validateName(orgName, "Organization name");
        validateName(username, "GitHub username");

        String trimmedOrg = orgName.trim();
        String trimmedUser = username.trim();

        // 1. Check direct public membership endpoint (GET /orgs/{org}/public_members/{username})
        boolean isPublic = githubClient.isPublicMember(trimmedOrg, trimmedUser);
        if (isPublic) {
            return new GithubMemberValidationResponse(
                    trimmedOrg,
                    trimmedUser,
                    true,
                    "User '" + trimmedUser + "' is a verified member of '" + trimmedOrg + "'."
            );
        }

        // 2. Fallback: check against fetched member logins in case of casing differences
        List<GithubMemberResponse> members = githubClient.getOrganizationMembers(trimmedOrg);
        boolean matched = members.stream()
                .anyMatch(m -> m.login() != null && m.login().equalsIgnoreCase(trimmedUser));

        if (matched) {
            return new GithubMemberValidationResponse(
                    trimmedOrg,
                    trimmedUser,
                    true,
                    "User '" + trimmedUser + "' is a verified member of '" + trimmedOrg + "'."
            );
        }

        return new GithubMemberValidationResponse(
                trimmedOrg,
                trimmedUser,
                false,
                "User '" + trimmedUser + "' was not found in the public member list of '" + trimmedOrg +
                        "'. If you are a member, please visit https://github.com/orgs/" + trimmedOrg +
                        "/people, find your name, and set your membership to 'Public'."
        );
    }

    /**
     * Validate organization membership by looking up the user's GitHub username from database using their Auth0 User ID.
     */
    public GithubMemberValidationResponse validateUserMembershipByAuth0UserId(String orgName, String auth0UserId) {
        validateName(orgName, "Organization name");
        validateName(auth0UserId, "Auth0 User ID");

        String trimmedAuth0Id = auth0UserId.trim();
        User user = userRepository.findByAuth0UserId(trimmedAuth0Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for Auth0 User ID: " + trimmedAuth0Id));

        String githubUsername = user.getGithubUsername();
        if (githubUsername == null || githubUsername.isBlank()) {
            throw new BadRequestException("No GitHub username registered for user with Auth0 ID: " + trimmedAuth0Id);
        }

        return validateUserMembership(orgName, githubUsername);
    }

    private void validateName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
    }
}