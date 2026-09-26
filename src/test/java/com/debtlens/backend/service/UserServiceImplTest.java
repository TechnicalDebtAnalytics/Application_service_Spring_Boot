package com.debtlens.backend.service;

import com.debtlens.backend.dto.response.UserResponseDTO;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.auth0.Auth0RoleResponse;
import com.debtlens.backend.mapper.UserMapper;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.security.Auth0UserService;
import com.debtlens.backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private UserRepository userRepository;
    private Auth0UserService auth0UserService;
    private UserMapper userMapper;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        auth0UserService = mock(Auth0UserService.class);
        userMapper = mock(UserMapper.class);
        service = new UserServiceImpl(userRepository, auth0UserService, userMapper);
    }

    @Test
    void currentUserMethods_shouldDelegateToAuthServiceAndMapProfileRoles() {
        User user = user(1L, "auth0|1");
        UserResponseDTO dto = mock(UserResponseDTO.class);
        when(auth0UserService.getAuthenticatedUser()).thenReturn(user);
        when(auth0UserService.getAuthenticatedUserRoles()).thenReturn(List.of(
                new Auth0RoleResponse("1", "ADMIN", null),
                new Auth0RoleResponse("2", "SYSTEM_USER", null)));
        when(auth0UserService.getAuthenticatedGithubUsername()).thenReturn("octocat");
        when(userMapper.toDTO(user, List.of("ADMIN", "SYSTEM_USER"))).thenReturn(dto);

        assertSame(user, service.getCurrentUser());
        assertSame(dto, service.getCurrentUserProfile());
        assertEquals("octocat", service.getCurrentUserGithubUsername());
    }

    @Test
    void getUserById_shouldMapUserWithLiveRoles() {
        User user = user(2L, "auth0|2");
        UserResponseDTO dto = mock(UserResponseDTO.class);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(auth0UserService.getUserRoles("auth0|2"))
                .thenReturn(List.of(new Auth0RoleResponse("1", "SYSTEM_USER", null)));
        when(userMapper.toDTO(user, List.of("SYSTEM_USER"))).thenReturn(dto);

        assertSame(dto, service.getUserById(2L));
    }

    @Test
    void getUserById_shouldFailWhenMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertEquals("User not found with ID: 404",
                assertThrows(ResourceNotFoundException.class, () -> service.getUserById(404L)).getMessage());
    }

    @Test
    void getUserByAuth0Id_shouldMapUserWithLiveRoles() {
        User user = user(2L, "auth0|2");
        UserResponseDTO dto = mock(UserResponseDTO.class);
        when(userRepository.findByAuth0UserId("auth0|2")).thenReturn(Optional.of(user));
        when(auth0UserService.getUserRoles("auth0|2")).thenReturn(List.of());
        when(userMapper.toDTO(user, List.of())).thenReturn(dto);

        assertSame(dto, service.getUserByAuth0Id("auth0|2"));
        verify(auth0UserService).getUserRoles("auth0|2");
    }

    @Test
    void getUserByAuth0Id_shouldFailWhenMissing() {
        when(userRepository.findByAuth0UserId("auth0|missing")).thenReturn(Optional.empty());

        assertEquals("User not found with Auth0 ID: auth0|missing",
                assertThrows(ResourceNotFoundException.class,
                        () -> service.getUserByAuth0Id("auth0|missing")).getMessage());
    }

    private static User user(Long id, String auth0Id) {
        User user = new User();
        user.setUserId(id);
        user.setAuth0UserId(auth0Id);
        return user;
    }
}
