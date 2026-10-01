package com.debtlens.backend.integration.github;

import com.debtlens.backend.config.GithubConfig;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.github.dto.GithubMemberResponse;
import com.debtlens.backend.integration.github.dto.GithubOrgResponse;
import com.debtlens.backend.integration.github.dto.GithubRepoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class GithubClient {

    private static final Logger log = LoggerFactory.getLogger(GithubClient.class);

    private final RestClient restClient;
    private final GithubAppTokenService githubAppTokenService;
    private final String defaultToken;

    public GithubClient(GithubConfig githubConfig, GithubAppTokenService githubAppTokenService) {
        this.githubAppTokenService = githubAppTokenService;
        this.defaultToken = githubConfig.token() != null && !githubConfig.token().isBlank()
                ? githubConfig.token().trim()
                : null;

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(githubConfig.apiUrl())
                .defaultHeader("User-Agent", "DebtLens-App")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        this.restClient = builder.build();

        if (this.defaultToken != null) {
            log.info("GitHub API client initialized with default fallback token.");
        } else {
            log.warn("GitHub API client initialized without default token.");
        }
    }

    private String resolveAuthHeader(Long installationId) {
        if (installationId != null && githubAppTokenService.isConfigured()) {
            try {
                String instToken = githubAppTokenService.getInstallationAccessToken(installationId);
                if (instToken != null && !instToken.isBlank()) {
                    return "Bearer " + instToken;
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve installation token for installationId {}: {}", installationId, ex.getMessage());
            }
        }
        if (defaultToken != null) {
            return "Bearer " + defaultToken;
        }
        return null;
    }

    /**
     * Fetch GitHub organization metadata.
     */
    public GithubOrgResponse getOrganization(String orgName) {
        return getOrganization(orgName, null);
    }

    public GithubOrgResponse getOrganization(String orgName, Long installationId) {
        try {
            var request = restClient.get()
                    .uri("/orgs/{org}", orgName)
                    .accept(MediaType.APPLICATION_JSON);

            String authHeader = resolveAuthHeader(installationId);
            if (authHeader != null) {
                request.header("Authorization", authHeader);
            }

            return request.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("GitHub organization '" + orgName + "' not found");
                    })
                    .body(GithubOrgResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("GitHub organization '" + orgName + "' not found");
        }
    }

    /**
     * Fetch repositories belonging to the organization.
     */
    public List<GithubRepoResponse> getOrganizationRepositories(String orgName) {
        return getOrganizationRepositories(orgName, null);
    }

    public List<GithubRepoResponse> getOrganizationRepositories(String orgName, Long installationId) {
        try {
            var request = restClient.get()
                    .uri("/orgs/{org}/repos?per_page=100&type=all&sort=updated", orgName)
                    .accept(MediaType.APPLICATION_JSON);

            String authHeader = resolveAuthHeader(installationId);
            if (authHeader != null) {
                request.header("Authorization", authHeader);
            }

            List<GithubRepoResponse> repos = request.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("GitHub organization '" + orgName + "' not found");
                    })
                    .body(new ParameterizedTypeReference<List<GithubRepoResponse>>() {});

            return repos != null ? repos : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("GitHub organization '" + orgName + "' not found");
        }
    }

    /**
     * Fetch public members of the organization.
     */
    public List<GithubMemberResponse> getOrganizationMembers(String orgName) {
        return getOrganizationMembers(orgName, null);
    }

    public List<GithubMemberResponse> getOrganizationMembers(String orgName, Long installationId) {
        try {
            var request = restClient.get()
                    .uri("/orgs/{org}/members?per_page=100", orgName)
                    .accept(MediaType.APPLICATION_JSON);

            String authHeader = resolveAuthHeader(installationId);
            if (authHeader != null) {
                request.header("Authorization", authHeader);
            }

            List<GithubMemberResponse> members = request.retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("GitHub organization '" + orgName + "' not found");
                    })
                    .body(new ParameterizedTypeReference<List<GithubMemberResponse>>() {});

            return members != null ? members : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("GitHub organization '" + orgName + "' not found");
        }
    }

    /**
     * Check if a specific user is a public member of the organization.
     * GitHub endpoint: GET /orgs/{org}/public_members/{username}
     * Returns 204 No Content if public member, 404 Not Found if not a public member.
     */
    public boolean isPublicMember(String orgName, String username) {
        return isPublicMember(orgName, username, null);
    }

    public boolean isPublicMember(String orgName, String username, Long installationId) {
        try {
            var request = restClient.get()
                    .uri("/orgs/{org}/public_members/{username}", orgName, username);

            String authHeader = resolveAuthHeader(installationId);
            if (authHeader != null) {
                request.header("Authorization", authHeader);
            }

            return request.exchange((req, res) -> {
                HttpStatusCode statusCode = res.getStatusCode();
                return statusCode.value() == 204 || statusCode.is2xxSuccessful();
            });
        } catch (Exception ex) {
            log.debug("GitHub public membership check failed for {} in {}: {}", username, orgName, ex.getMessage());
            return false;
        }
    }

    /**
     * Fetch contributors for a specific repository across ALL branches.
     * Combines default branch contributors with authors from all active branches.
     */
    public List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> getRepoContributors(String owner, String repo) {
        return getRepoContributors(owner, repo, null);
    }

    public List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> getRepoContributors(String owner, String repo, Long installationId) {
        String cleanOwner = owner.trim();
        String cleanRepo = repo.trim();
        java.util.Map<String, com.debtlens.backend.integration.github.dto.GithubContributorResponse> contributorsByLogin = new java.util.LinkedHashMap<>();
        String authHeader = resolveAuthHeader(installationId);

        try {
            // 1. Fetch contributors from default contributors endpoint
            try {
                var request = restClient.get()
                        .uri("/repos/{owner}/{repo}/contributors?per_page=100", cleanOwner, cleanRepo)
                        .accept(MediaType.APPLICATION_JSON);
                if (authHeader != null) {
                    request.header("Authorization", authHeader);
                }

                List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> baseContributors = request.retrieve()
                        .onStatus(status -> status.value() == 404, (req, res) -> {
                            throw new ResourceNotFoundException("Repository '" + cleanOwner + "/" + cleanRepo + "' not found on GitHub");
                        })
                        .body(new ParameterizedTypeReference<List<com.debtlens.backend.integration.github.dto.GithubContributorResponse>>() {});

                if (baseContributors != null) {
                    for (com.debtlens.backend.integration.github.dto.GithubContributorResponse c : baseContributors) {
                        if (c != null && c.login() != null && !c.login().isBlank()) {
                            contributorsByLogin.put(c.login().toLowerCase(), c);
                        }
                    }
                }
            } catch (HttpClientErrorException.NotFound ex) {
                throw new ResourceNotFoundException("Repository '" + cleanOwner + "/" + cleanRepo + "' not found on GitHub");
            } catch (ResourceNotFoundException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("Failed base contributor fetch for {}/{}: {}", cleanOwner, cleanRepo, ex.getMessage());
            }

            // 2. Fetch all branches of the repository so contributors across every branch are included
            try {
                var branchRequest = restClient.get()
                        .uri("/repos/{owner}/{repo}/branches?per_page=100", cleanOwner, cleanRepo)
                        .accept(MediaType.APPLICATION_JSON);
                if (authHeader != null) {
                    branchRequest.header("Authorization", authHeader);
                }

                List<com.debtlens.backend.integration.github.dto.GithubBranchResponse> branches = branchRequest.retrieve()
                        .body(new ParameterizedTypeReference<List<com.debtlens.backend.integration.github.dto.GithubBranchResponse>>() {});

                if (branches != null && !branches.isEmpty()) {
                    for (com.debtlens.backend.integration.github.dto.GithubBranchResponse branch : branches) {
                        if (branch == null || branch.name() == null || branch.name().isBlank()) continue;
                        try {
                            var commitRequest = restClient.get()
                                    .uri("/repos/{owner}/{repo}/commits?sha={branch}&per_page=100", cleanOwner, cleanRepo, branch.name())
                                    .accept(MediaType.APPLICATION_JSON);
                            if (authHeader != null) {
                                commitRequest.header("Authorization", authHeader);
                            }

                            List<com.debtlens.backend.integration.github.dto.GithubCommitItem> commits = commitRequest.retrieve()
                                    .body(new ParameterizedTypeReference<List<com.debtlens.backend.integration.github.dto.GithubCommitItem>>() {});

                            if (commits != null) {
                                for (com.debtlens.backend.integration.github.dto.GithubCommitItem commit : commits) {
                                    if (commit == null) continue;
                                    com.debtlens.backend.integration.github.dto.GithubCommitItem.GithubCommitUser user =
                                            commit.author() != null && commit.author().login() != null ? commit.author() : commit.committer();

                                    if (user != null && user.login() != null && !user.login().isBlank()) {
                                        String loginKey = user.login().toLowerCase();
                                        if (!contributorsByLogin.containsKey(loginKey)) {
                                            contributorsByLogin.put(loginKey, new com.debtlens.backend.integration.github.dto.GithubContributorResponse(
                                                    user.id(),
                                                    user.login(),
                                                    user.avatarUrl(),
                                                    user.htmlUrl(),
                                                    1,
                                                    user.type() != null ? user.type() : "User"
                                            ));
                                        } else {
                                            com.debtlens.backend.integration.github.dto.GithubContributorResponse existing = contributorsByLogin.get(loginKey);
                                            int count = existing.contributions() != null ? existing.contributions() : 0;
                                            contributorsByLogin.put(loginKey, new com.debtlens.backend.integration.github.dto.GithubContributorResponse(
                                                    existing.id() != null ? existing.id() : user.id(),
                                                    existing.login(),
                                                    existing.avatarUrl() != null ? existing.avatarUrl() : user.avatarUrl(),
                                                    existing.htmlUrl() != null ? existing.htmlUrl() : user.htmlUrl(),
                                                    count + 1,
                                                    existing.type() != null ? existing.type() : "User"
                                            ));
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            log.debug("Could not fetch commits for branch {} of {}/{}: {}", branch.name(), cleanOwner, cleanRepo, ex.getMessage());
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("Could not fetch branches for repository {}/{}: {}", cleanOwner, cleanRepo, ex.getMessage());
            }

            return new java.util.ArrayList<>(contributorsByLogin.values());
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to fetch repository-wide contributors for {}/{}: {}", cleanOwner, cleanRepo, ex.getMessage());
            return new java.util.ArrayList<>(contributorsByLogin.values());
        }
    }
}