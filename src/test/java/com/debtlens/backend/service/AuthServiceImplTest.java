package com.debtlens.backend.service;

import com.debtlens.backend.config.Auth0RoleConfig;
import com.debtlens.backend.dto.request.RegisterRequest;
import com.debtlens.backend.dto.response.RegistrationResponse;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.integration.auth0.Auth0Client;
import com.debtlens.backend.integration.auth0.Auth0CreateUserResponse;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private Auth0Client auth0Client;
    private Auth0RoleConfig roleConfig;
    private UserRepository userRepository;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        auth0Client = mock(Auth0Client.class);
        roleConfig = new Auth0RoleConfig();
        roleConfig.setSystemUser("role-system-user");
        userRepository = mock(UserRepository.class);
        service = new AuthServiceImpl(auth0Client, roleConfig, userRepository);
    }

    @Test
    void register_shouldCreateAuth0AndLocalUserAndReturnRegistration() {
        RegisterRequest request = request("ada@example.com", "ada-gh");
        when(auth0Client.getManagementApiToken()).thenReturn("management-token");
        when(auth0Client.createUser(
                "management-token", "ada@example.com", "password123", "Ada", "Lovelace"))
                .thenReturn(new Auth0CreateUserResponse("auth0|123", "ada@example.com", false));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = service.register(request);

        verify(userRepository).existsByEmail("ada@example.com");
        verify(userRepository).existsByGithubUsername("ada-gh");
        verify(auth0Client).assignRole("management-token", "auth0|123", "role-system-user");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("auth0|123", savedUser.getAuth0UserId());
        assertEquals("Ada", savedUser.getFirstName());
        assertEquals("Lovelace", savedUser.getLastName());
        assertEquals("ada@example.com", savedUser.getEmail());
        assertEquals("ada-gh", savedUser.getGithubUsername());
        assertFalse(savedUser.getEmailVerified());

        assertEquals("Registration successful", response.message());
        assertEquals("auth0|123", response.auth0UserId());
        assertEquals("ada@example.com", response.email());
        assertEquals("SYSTEM_USER", response.role());
        assertEquals("ada-gh", response.githubUsername());
    }

    @Test
    void register_shouldRejectDuplicateEmailBeforeCallingAuth0() {
        RegisterRequest request = request("ada@example.com", "ada-gh");
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.register(request));

        assertEquals("A user with this email already exists", exception.getMessage());
        verify(userRepository, never()).existsByGithubUsername(any());
        verifyNoInteractions(auth0Client);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldTrimGithubUsernameForDuplicateCheck() {
        RegisterRequest request = request("ada@example.com", "  ada-gh  ");
        when(userRepository.existsByGithubUsername("ada-gh")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.register(request));

        assertEquals("A user with this GitHub username already exists", exception.getMessage());
        verify(userRepository).existsByGithubUsername("ada-gh");
        verifyNoInteractions(auth0Client);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectMissingSystemUserRoleConfiguration() {
        roleConfig.setSystemUser("  ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.register(request("ada@example.com", "ada-gh"))
        );

        assertEquals("SYSTEM_USER Auth0 role ID is not configured", exception.getMessage());
        verifyNoInteractions(auth0Client);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectAuth0ResponseWithoutUserId() {
        when(auth0Client.getManagementApiToken()).thenReturn("management-token");
        when(auth0Client.createUser(any(), any(), any(), any(), any()))
                .thenReturn(new Auth0CreateUserResponse(" ", "ada@example.com", false));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.register(request("ada@example.com", "ada-gh"))
        );

        assertEquals("Auth0 user creation failed: no Auth0 user ID returned", exception.getMessage());
        verify(auth0Client, never()).assignRole(any(), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldPropagateManagementTokenFailureWithoutCreatingLocalUser() {
        when(auth0Client.getManagementApiToken()).thenThrow(new RuntimeException("Auth0 unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.register(request("ada@example.com", "ada-gh"))
        );

        assertEquals("Auth0 unavailable", exception.getMessage());
        verify(auth0Client, never()).createUser(any(), any(), any(), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldPropagateRoleAssignmentFailureWithoutCreatingLocalUser() {
        when(auth0Client.getManagementApiToken()).thenReturn("management-token");
        when(auth0Client.createUser(any(), any(), any(), any(), any()))
                .thenReturn(new Auth0CreateUserResponse("auth0|123", "ada@example.com", false));
        org.mockito.Mockito.doThrow(new RuntimeException("role assignment failed"))
                .when(auth0Client).assignRole("management-token", "auth0|123", "role-system-user");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.register(request("ada@example.com", "ada-gh"))
        );

        assertEquals("role assignment failed", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    private static RegisterRequest request(String email, String githubUsername) {
        return new RegisterRequest(email, "password123", "Ada", "Lovelace", githubUsername);
    }
}
