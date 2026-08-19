package com.debtlens.backend.security;

import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.auth0.Auth0Client;
import com.debtlens.backend.integration.auth0.Auth0RoleResponse;
import com.debtlens.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Auth0UserService {

    private final UserRepository userRepository;
    private final Auth0Client auth0Client;

    public Auth0UserService(UserRepository userRepository, Auth0Client auth0Client) {
        this.userRepository = userRepository;
        this.auth0Client = auth0Client;
    }

    /**
     * Extracts the Auth0 User ID ('sub' claim) from the Spring Security context.
     */
    public String getAuthenticatedAuth0UserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("No authenticated security context found");
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return sub;
            }
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return sub;
            }
        }

        String name = authentication.getName();
        if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
            return name;
        }

        throw new BadRequestException("Unable to extract Auth0 User ID from JWT token");
    }

    /**
     * Finds and returns the local User entity from PostgreSQL matching the authenticated Auth0 ID.
     */
    public User getAuthenticatedUser() {
        String auth0UserId = getAuthenticatedAuth0UserId();
        return userRepository.findByAuth0UserId(auth0UserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with Auth0 ID '" + auth0UserId + "' not found in database"
                ));
    }

    /**
     * Retrieves the GitHub username associated with the authenticated user.
     */
    public String getAuthenticatedGithubUsername() {
        User user = getAuthenticatedUser();
        String githubUsername = user.getGithubUsername();

        if (githubUsername == null || githubUsername.isBlank()) {
            throw new BadRequestException("Authenticated user has no GitHub username registered");
        }

        return githubUsername;
    }

    /**
     * Retrieves live roles directly from Auth0 Management API for a given Auth0 user ID.
     */
    public List<Auth0RoleResponse> getUserRoles(String auth0UserId) {
        if (auth0UserId == null || auth0UserId.isBlank()) {
            throw new BadRequestException("Auth0 User ID must not be blank");
        }
        String managementToken = auth0Client.getManagementApiToken();
        return auth0Client.getUserRoles(managementToken, auth0UserId);
    }

    /**
     * Retrieves live roles directly from Auth0 Management API for the current authenticated user.
     */
    public List<Auth0RoleResponse> getAuthenticatedUserRoles() {
        String auth0UserId = getAuthenticatedAuth0UserId();
        return getUserRoles(auth0UserId);
    }

    /**
     * Checks if the currently authenticated user holds a specific Auth0 role name.
     */
    public boolean hasRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        List<Auth0RoleResponse> roles = getAuthenticatedUserRoles();
        return roles.stream()
                .anyMatch(r -> r.name() != null && r.name().equalsIgnoreCase(roleName.trim()));
    }

    /**
     * Validates that the current user has the required role, throwing AccessDeniedException if not.
     */
    public void requireRole(String roleName) {
        if (!hasRole(roleName)) {
            throw new AccessDeniedException("Access denied: User does not have required Auth0 role '" + roleName + "'");
        }
    }

    /**
     * Checks if the user has an Admin or Super Admin role in Auth0.
     */
    public boolean isAdmin() {
        List<Auth0RoleResponse> roles = getAuthenticatedUserRoles();
        return roles.stream()
                .anyMatch(r -> r.name() != null &&
                        (r.name().equalsIgnoreCase("SUPER_ADMIN") ||
                         r.name().equalsIgnoreCase("ADMIN") ||
                         r.name().equalsIgnoreCase("System_Admin")));
    }
}