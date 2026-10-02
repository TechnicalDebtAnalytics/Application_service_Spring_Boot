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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import com.debtlens.backend.config.CacheConfig;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GithubService {

    private final GithubClient githubClient;
    private final CacheManager cacheManager;
    private final GithubAppTokenService githubAppTokenService;
    private final UserRepository userRepository;

    public GithubService(GithubClient githubClient, GithubAppTokenService githubAppTokenService, UserRepository userRepository, CacheManager cacheManager) {
        this.githubClient = githubClient;
        this.cacheManager = cacheManager;
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
        return cached(CacheConfig.CACHE_GITHUB_ORGANIZATIONS, key(orgName, installationId),
                () -> githubClient.getOrganization(orgName.trim(), installationId));
    }

    /**
     * Get organization repositories for repository selection.
     */
    public List<GithubRepoResponse> getRepositories(String orgName, Long installationId) {
        validateName(orgName, "Organization name");
        return cached(CacheConfig.CACHE_GITHUB_REPOSITORIES, key(orgName, installationId),
                () -> githubClient.getOrganizationRepositories(orgName.trim(), installationId));
    }

    public List<GithubRepoResponse> getRepositories(String orgName) {
        return getRepositories(orgName, null);
    }

    /**
     * Get public members of an organization.
     */
    public List<GithubMemberResponse> getMembers(String orgName, Long installationId) {
        validateName(orgName, "Organization name");
        return cached(CacheConfig.CACHE_GITHUB_MEMBERS, key(orgName, installationId),
                () -> githubClient.getOrganizationMembers(orgName.trim(), installationId));
    }

    public List<GithubMemberResponse> getMembers(String orgName) {
        return getMembers(orgName, null);
    }

    /**
     * Get contributors for a specific repository.
     */
    public List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> getContributors(String owner, String repo, Long installationId) {
        validateName(owner, "Repository owner / organization");
        validateName(repo, "Repository name");
        return cached(CacheConfig.CACHE_GITHUB_CONTRIBUTORS,
                new SimpleKey(normalize(owner), normalize(repo), installationId != null ? installationId : "default"),
                () -> githubClient.getRepoContributors(owner.trim(), repo.trim(), installationId));
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
        List<GithubMemberResponse> members = getMembers(trimmedOrg, installationId);
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
        List<GithubRepoResponse> repos = getRepositories(trimmedOrg, installationId);
        for (GithubRepoResponse repo : repos) {
            if (repo.name() != null) {
                List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> contribs =
                        getContributors(trimmedOrg, repo.name(), installationId);
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

    // Programmatic caching also applies to internal membership-validation calls.
    // Cache.get(key, loader) coalesces concurrent misses in Caffeine.
    private <T> T cached(String name, Object key, Callable<T> loader) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(name), "Missing cache: " + name);
        try {
            return cache.get(key, loader);
        } catch (Cache.ValueRetrievalException ex) {
            if (ex.getCause() instanceof RuntimeException cause) throw cause;
            throw ex;
        }
    }

    private Object key(String orgName, Long installationId) {
        return new SimpleKey(normalize(orgName), installationId != null ? installationId : "default");
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void validateName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
    }
}