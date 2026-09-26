package com.debtlens.backend.security;

import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.auth0.Auth0Client;
import com.debtlens.backend.integration.auth0.Auth0RoleResponse;
import com.debtlens.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Auth0UserServiceTest {

    private UserRepository userRepository;
    private Auth0Client auth0Client;
    private Auth0UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        auth0Client = mock(Auth0Client.class);
        service = new Auth0UserService(userRepository, auth0Client);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthenticatedAuth0UserId_shouldUseJwtAuthenticationTokenSubject() {
        Jwt jwt = jwt("auth0|jwt-user");
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getToken()).thenReturn(jwt);
        authenticate(authentication);

        assertEquals("auth0|jwt-user", service.getAuthenticatedAuth0UserId());
    }

    @Test
    void getAuthenticatedAuth0UserId_shouldUseJwtPrincipalSubject() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt("auth0|principal-user"));
        authenticate(authentication);

        assertEquals("auth0|principal-user", service.getAuthenticatedAuth0UserId());
    }

    @Test
    void getAuthenticatedAuth0UserId_shouldFallBackToAuthenticationName() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("principal");
        when(authentication.getName()).thenReturn("auth0|named-user");
        authenticate(authentication);

        assertEquals("auth0|named-user", service.getAuthenticatedAuth0UserId());
    }

    @Test
    void getAuthenticatedAuth0UserId_shouldRejectMissingSecurityContextAuthentication() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                service::getAuthenticatedAuth0UserId
        );

        assertEquals("No authenticated security context found", exception.getMessage());
    }

    @Test
    void getAuthenticatedAuth0UserId_shouldRejectAnonymousAuthentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("anonymousUser");
        authenticate(authentication);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                service::getAuthenticatedAuth0UserId
        );

        assertEquals("Unable to extract Auth0 User ID from JWT token", exception.getMessage());
    }

    @Test
    void getAuthenticatedUser_shouldResolveLocalUserByAuthenticatedAuth0Id() {
        authenticateByName("auth0|123");
        User user = new User();
        user.setAuth0UserId("auth0|123");
        when(userRepository.findByAuth0UserId("auth0|123")).thenReturn(Optional.of(user));

        assertSame(user, service.getAuthenticatedUser());
    }

    @Test
    void getAuthenticatedUser_shouldFailWhenLocalUserDoesNotExist() {
        authenticateByName("auth0|missing");
        when(userRepository.findByAuth0UserId("auth0|missing")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                service::getAuthenticatedUser
        );

        assertEquals("User with Auth0 ID 'auth0|missing' not found in database", exception.getMessage());
    }

    @Test
    void getAuthenticatedGithubUsername_shouldReturnRegisteredUsername() {
        authenticateByName("auth0|123");
        User user = new User();
        user.setGithubUsername("octocat");
        when(userRepository.findByAuth0UserId("auth0|123")).thenReturn(Optional.of(user));

        assertEquals("octocat", service.getAuthenticatedGithubUsername());
    }

    @Test
    void getAuthenticatedGithubUsername_shouldRejectBlankUsername() {
        authenticateByName("auth0|123");
        User user = new User();
        user.setGithubUsername("  ");
        when(userRepository.findByAuth0UserId("auth0|123")).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                service::getAuthenticatedGithubUsername
        );

        assertEquals("Authenticated user has no GitHub username registered", exception.getMessage());
    }

    @Test
    void getUserRoles_shouldUseManagementTokenAndRequestedUserId() {
        List<Auth0RoleResponse> roles = List.of(role("ADMIN"));
        when(auth0Client.getManagementApiToken()).thenReturn("management-token");
        when(auth0Client.getUserRoles("management-token", "auth0|123")).thenReturn(roles);

        assertSame(roles, service.getUserRoles("auth0|123"));
        verify(auth0Client).getUserRoles("management-token", "auth0|123");
    }

    @Test
    void getUserRoles_shouldRejectBlankUserIdWithoutCallingAuth0() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.getUserRoles("  ")
        );

        assertEquals("Auth0 User ID must not be blank", exception.getMessage());
        verifyNoInteractions(auth0Client);
    }

    @Test
    void hasRole_shouldMatchTrimmedRoleNameIgnoringCase() {
        stubAuthenticatedRoles(role("SYSTEM_USER"), role("ADMIN"));

        assertTrue(service.hasRole("  admin  "));
    }

    @Test
    void hasRole_shouldReturnFalseForMissingRole() {
        stubAuthenticatedRoles(role("SYSTEM_USER"));

        assertFalse(service.hasRole("ADMIN"));
    }

    @Test
    void hasRole_shouldReturnFalseForBlankRoleWithoutCallingAuth0() {
        assertFalse(service.hasRole("  "));
        verifyNoInteractions(auth0Client);
    }

    @Test
    void requireRole_shouldReturnNormallyWhenRoleExists() {
        stubAuthenticatedRoles(role("ADMIN"));

        service.requireRole("ADMIN");
    }

    @Test
    void requireRole_shouldThrowAccessDeniedWhenRoleIsMissing() {
        stubAuthenticatedRoles(role("SYSTEM_USER"));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> service.requireRole("ADMIN")
        );

        assertEquals("Access denied: User does not have required Auth0 role 'ADMIN'", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "admin", "SUPER_ADMIN", "System_Admin"})
    void isAdmin_shouldRecognizeSupportedAdminRoleNames(String roleName) {
        stubAuthenticatedRoles(role(roleName));

        assertTrue(service.isAdmin());
    }

    @Test
    void isAdmin_shouldReturnFalseWithoutAnAdminRole() {
        stubAuthenticatedRoles(role("SYSTEM_USER"), new Auth0RoleResponse("role-null", null, null));

        assertFalse(service.isAdmin());
    }

    private void stubAuthenticatedRoles(Auth0RoleResponse... roles) {
        authenticateByName("auth0|123");
        when(auth0Client.getManagementApiToken()).thenReturn("management-token");
        when(auth0Client.getUserRoles("management-token", "auth0|123")).thenReturn(List.of(roles));
    }

    private static Auth0RoleResponse role(String name) {
        return new Auth0RoleResponse("role-" + name, name, null);
    }

    private static Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }

    private static void authenticateByName(String name) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(name);
        authenticate(authentication);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
