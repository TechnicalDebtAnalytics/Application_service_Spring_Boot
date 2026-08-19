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

    public GithubClient(GithubConfig githubConfig) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(githubConfig.apiUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (githubConfig.token() != null && !githubConfig.token().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + githubConfig.token().trim());
            log.info("GitHub API client initialized with configured Personal Access Token.");
        } else {
            log.warn("GitHub API client initialized without token. Unauthenticated rate limits (60 req/hr) apply.");
        }

        this.restClient = builder.build();
    }

    /**
     * Fetch GitHub organization metadata.
     */
    public GithubOrgResponse getOrganization(String orgName) {
        try {
            return restClient.get()
                    .uri("/orgs/{org}", orgName)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
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
        try {
            List<GithubRepoResponse> repos = restClient.get()
                    .uri("/orgs/{org}/repos?per_page=100&type=all&sort=updated", orgName)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
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
        try {
            List<GithubMemberResponse> members = restClient.get()
                    .uri("/orgs/{org}/members?per_page=100", orgName)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
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
        try {
            return restClient.get()
                    .uri("/orgs/{org}/public_members/{username}", orgName, username)
                    .exchange((req, res) -> {
                        HttpStatusCode statusCode = res.getStatusCode();
                        return statusCode.value() == 204 || statusCode.is2xxSuccessful();
                    });
        } catch (Exception ex) {
            log.debug("GitHub public membership check failed for {} in {}: {}", username, orgName, ex.getMessage());
            return false;
        }
    }

    /**
     * Fetch contributors for a specific repository.
     * GitHub endpoint: GET /repos/{owner}/{repo}/contributors?per_page=100
     */
    public List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> getRepoContributors(String owner, String repo) {
        try {
            List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> contributors = restClient.get()
                    .uri("/repos/{owner}/{repo}/contributors?per_page=100", owner, repo)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ResourceNotFoundException("Repository '" + owner + "/" + repo + "' not found on GitHub");
                    })
                    .body(new ParameterizedTypeReference<List<com.debtlens.backend.integration.github.dto.GithubContributorResponse>>() {});

            return contributors != null ? contributors : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Repository '" + owner + "/" + repo + "' not found on GitHub");
        } catch (Exception ex) {
            log.warn("Failed to fetch contributors for {}/{}: {}", owner, repo, ex.getMessage());
            return Collections.emptyList();
        }
    }
}