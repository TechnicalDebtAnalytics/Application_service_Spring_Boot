package com.debtlens.backend.controller;

import com.debtlens.backend.integration.github.GithubService;
import com.debtlens.backend.integration.github.dto.GithubAppInfoResponse;
import com.debtlens.backend.integration.github.dto.GithubContributorResponse;
import com.debtlens.backend.integration.github.dto.GithubMemberResponse;
import com.debtlens.backend.integration.github.dto.GithubMemberValidationResponse;
import com.debtlens.backend.integration.github.dto.GithubOrgResponse;
import com.debtlens.backend.integration.github.dto.GithubRepoResponse;
import com.debtlens.backend.security.Auth0UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/github")
public class GithubController {

    private final GithubService githubService;
    private final Auth0UserService auth0UserService;

    public GithubController(GithubService githubService, Auth0UserService auth0UserService) {
        this.githubService = githubService;
        this.auth0UserService = auth0UserService;
    }

    @GetMapping("/app/info")
    public ResponseEntity<GithubAppInfoResponse> getAppInfo() {
        return ResponseEntity.ok(githubService.getAppInfo());
    }

    @GetMapping("/orgs/{orgName}")
    public ResponseEntity<GithubOrgResponse> getOrganization(
            @PathVariable String orgName,
            @RequestParam(required = false) Long installationId
    ) {
        GithubOrgResponse org = installationId != null
                ? githubService.getOrganization(orgName, installationId)
                : githubService.getOrganization(orgName);
        return ResponseEntity.ok(org);
    }

    @GetMapping("/orgs/{orgName}/repos")
    public ResponseEntity<List<GithubRepoResponse>> getRepositories(
            @PathVariable String orgName,
            @RequestParam(required = false) Long installationId
    ) {
        List<GithubRepoResponse> repos = installationId != null
                ? githubService.getRepositories(orgName, installationId)
                : githubService.getRepositories(orgName);
        return ResponseEntity.ok(repos);
    }

    @GetMapping("/orgs/{orgName}/members")
    public ResponseEntity<List<GithubMemberResponse>> getMembers(
            @PathVariable String orgName,
            @RequestParam(required = false) Long installationId
    ) {
        List<GithubMemberResponse> members = installationId != null
                ? githubService.getMembers(orgName, installationId)
                : githubService.getMembers(orgName);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/repos/{owner}/{repo}/contributors")
    public ResponseEntity<List<GithubContributorResponse>> getContributors(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false) Long installationId
    ) {
        List<GithubContributorResponse> contributors = installationId != null
                ? githubService.getContributors(owner, repo, installationId)
                : githubService.getContributors(owner, repo);
        return ResponseEntity.ok(contributors);
    }

    /**
     * Validate organization membership of the currently authenticated user.
     * Extracts auth0UserId securely from the JWT token, retrieves their registered
     * GitHub username from the database, and verifies their membership in the GitHub org.
     */
    @GetMapping("/orgs/{orgName}/validate-my-membership")
    public ResponseEntity<GithubMemberValidationResponse> validateMyMembership(
            @PathVariable String orgName,
            @RequestParam(required = false) Long installationId
    ) {
        String auth0UserId = auth0UserService.getAuthenticatedAuth0UserId();
        GithubMemberValidationResponse result = installationId != null
                ? githubService.validateUserMembershipByAuth0UserId(orgName, auth0UserId, installationId)
                : githubService.validateUserMembershipByAuth0UserId(orgName, auth0UserId);
        return ResponseEntity.ok(result);
    }
}
