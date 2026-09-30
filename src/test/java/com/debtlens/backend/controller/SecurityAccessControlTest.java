package com.debtlens.backend.controller;

import com.debtlens.backend.config.SecurityConfig;
import com.debtlens.backend.dto.response.UserResponseDTO;
import com.debtlens.backend.exception.BadRequestException;
import com.debtlens.backend.exception.GlobalExceptionHandler;
import com.debtlens.backend.integration.github.GithubService;
import com.debtlens.backend.security.Auth0UserService;
import com.debtlens.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        GithubController.class,
        UserController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SecurityAccessControlTest {

    private static final String INVALID_TOKEN = "malformed-token";
    private static final String USER_TOKEN = "system-user-token";
    private static final String ADMIN_TOKEN = "system-admin-token";
    private static final String ROLES_CLAIM = "https://debtlens.example.com/roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private GithubService githubService;

    @MockitoBean(name = "auth0UserService")
    private Auth0UserService auth0UserService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void configureSyntheticJwtDecoding() {
        when(jwtDecoder.decode(INVALID_TOKEN))
                .thenThrow(new BadJwtException("Malformed test JWT"));
        when(jwtDecoder.decode(USER_TOKEN))
                .thenReturn(jwt(USER_TOKEN, "auth0|requesting-user", List.of("SYSTEM_USER")));
        when(jwtDecoder.decode(ADMIN_TOKEN))
                .thenReturn(jwt(ADMIN_TOKEN, "auth0|system-admin", List.of("SYSTEM_ADMIN")));
    }

    @Test
    void sec03_invalidJwtCannotAccessAuthenticatedProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(INVALID_TOKEN)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void authenticatedUserCanReadCurrentProfile() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(ownUserProfile());

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUserCannotReadProfileById() throws Exception {
        mockMvc.perform(get("/api/users/100"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void systemUserCanReadOwnProfileById() throws Exception {
        when(auth0UserService.isCurrentUser(100L)).thenReturn(true);
        when(userService.getUserById(100L)).thenReturn(ownUserProfile());

        mockMvc.perform(get("/api/users/100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    void sec06_anonymousGithubMembershipValidationRequiresAuthentication() throws Exception {
        when(auth0UserService.getAuthenticatedAuth0UserId())
                .thenThrow(new BadRequestException("No authenticated security context found"));

        mockMvc.perform(get("/api/github/orgs/debtlens/validate-my-membership"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(githubService);
    }

    @Test
    void sec07_systemUserCannotReadAnotherUsersProfile() throws Exception {
        when(auth0UserService.isCurrentUser(200L)).thenReturn(false);

        mockMvc.perform(get("/api/users/200")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void systemAdminHasNoImplicitAccessToAnotherUsersProfile() throws Exception {
        when(auth0UserService.isCurrentUser(200L)).thenReturn(false);

        mockMvc.perform(get("/api/users/200")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_TOKEN)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static Jwt jwt(String token, String subject, List<String> roles) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim(ROLES_CLAIM, roles)
                .build();
    }

    private static UserResponseDTO ownUserProfile() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 27, 12, 0);
        return new UserResponseDTO(
                100L,
                "auth0|requesting-user",
                "Requesting",
                "User",
                "requesting@example.test",
                "requesting-user",
                true,
                List.of("SYSTEM_USER"),
                now,
                now
        );
    }
}
