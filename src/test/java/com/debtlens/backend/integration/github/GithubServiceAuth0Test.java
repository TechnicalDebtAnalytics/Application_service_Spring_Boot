package com.debtlens.backend.integration.github;

import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.integration.github.dto.GithubMemberValidationResponse;
import com.debtlens.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubServiceAuth0Test {

    @Mock
    private GithubClient githubClient;

    @Mock
    private UserRepository userRepository;

    private GithubService githubService;

    @BeforeEach
    void setUp() {
        githubService = new GithubService(githubClient, userRepository);
    }

    @Test
    void validateUserMembershipByAuth0UserId_success() {
        User user = new User();
        user.setAuth0UserId("auth0|12345");
        user.setGithubUsername("octocat");

        when(userRepository.findByAuth0UserId("auth0|12345")).thenReturn(Optional.of(user));
        when(githubClient.isPublicMember("octo-org", "octocat")).thenReturn(true);

        GithubMemberValidationResponse response = githubService.validateUserMembershipByAuth0UserId("octo-org", "auth0|12345");

        assertNotNull(response);
        assertEquals("octocat", response.username());
        assertTrue(response.isMember());
        verify(userRepository, times(1)).findByAuth0UserId("auth0|12345");
        verify(githubClient, times(1)).isPublicMember("octo-org", "octocat");
    }

    @Test
    void validateUserMembershipByAuth0UserId_userNotFound() {
        when(userRepository.findByAuth0UserId("auth0|unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                githubService.validateUserMembershipByAuth0UserId("octo-org", "auth0|unknown")
        );
    }

    @Test
    void validateUserMembershipByAuth0UserId_missingGithubUsername() {
        User user = new User();
        user.setAuth0UserId("auth0|12345");
        user.setGithubUsername(null);

        when(userRepository.findByAuth0UserId("auth0|12345")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () ->
                githubService.validateUserMembershipByAuth0UserId("octo-org", "auth0|12345")
        );
    }
}
