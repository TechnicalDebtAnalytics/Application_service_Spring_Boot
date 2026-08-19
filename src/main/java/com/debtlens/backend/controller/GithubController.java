package com.debtlens.backend.controller;

import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.integration.github.GithubService;
import com.debtlens.backend.integration.github.dto.GithubMemberResponse;
import com.debtlens.backend.integration.github.dto.GithubMemberValidationResponse;
import com.debtlens.backend.integration.github.dto.GithubOrgResponse;
import com.debtlens.backend.integration.github.dto.GithubRepoResponse;
import com.debtlens.backend.security.Auth0UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @GetMapping("/orgs/{orgName}")
    public ResponseEntity<GithubOrgResponse> getOrganization(
            @PathVariable String orgName
    ) {
        GithubOrgResponse org = githubService.getOrganization(orgName);
        return ResponseEntity.ok(org);
    }

    @GetMapping("/orgs/{orgName}/repos")
    public ResponseEntity<List<GithubRepoResponse>> getRepositories(
            @PathVariable String orgName
    ) {
        List<GithubRepoResponse> repos = githubService.getRepositories(orgName);
        return ResponseEntity.ok(repos);
    }

    @GetMapping("/orgs/{orgName}/members")
    public ResponseEntity<List<GithubMemberResponse>> getMembers(
            @PathVariable String orgName
    ) {
        List<GithubMemberResponse> members = githubService.getMembers(orgName);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/repos/{owner}/{repo}/contributors")
    public ResponseEntity<List<com.debtlens.backend.integration.github.dto.GithubContributorResponse>> getContributors(
            @PathVariable String owner,
            @PathVariable String repo
    ) {
        List<com.debtlens.backend.integration.github.dto.GithubContributorResponse> contributors = githubService.getContributors(owner, repo);
        return ResponseEntity.ok(contributors);
    }

    /**
     * Validate membership by query param (username or auth0UserId) or JWT context.
     */
    @GetMapping("/orgs/{orgName}/validate-member")
    public ResponseEntity<GithubMemberValidationResponse> validateMember(
            @PathVariable String orgName,
            @RequestParam(required = false) String auth0UserId,
            @RequestParam(required = false) String username,
            Authentication authentication
    ) {
        String resolvedAuth0UserId = auth0UserId;

        if ((resolvedAuth0UserId == null || resolvedAuth0UserId.isBlank())
                && authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                resolvedAuth0UserId = jwt.getSubject();
            } else {
                resolvedAuth0UserId = authentication.getName();
            }
        }

        if (resolvedAuth0UserId != null && !resolvedAuth0UserId.isBlank()) {
            GithubMemberValidationResponse result = githubService.validateUserMembershipByAuth0UserId(orgName, resolvedAuth0UserId);
            return ResponseEntity.ok(result);
        }

        if (username != null && !username.isBlank()) {
            GithubMemberValidationResponse result = githubService.validateUserMembership(orgName, username);
            return ResponseEntity.ok(result);
        }

        throw new BadRequestException("Either auth0UserId, username, or an authenticated JWT token is required to validate membership");
    }

    /**
     * Validate membership of the currently authenticated user automatically via JWT token.
     */
    @GetMapping("/orgs/{orgName}/validate-my-membership")
    public ResponseEntity<GithubMemberValidationResponse> validateMyMembership(
            @PathVariable String orgName
    ) {
        String auth0UserId = auth0UserService.getAuthenticatedAuth0UserId();
        GithubMemberValidationResponse result = githubService.validateUserMembershipByAuth0UserId(orgName, auth0UserId);
        return ResponseEntity.ok(result);
    }
}
