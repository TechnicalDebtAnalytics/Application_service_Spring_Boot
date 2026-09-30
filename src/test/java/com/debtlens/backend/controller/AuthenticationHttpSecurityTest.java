package com.debtlens.backend.controller;

import com.debtlens.backend.config.SecurityConfig;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        GithubController.class,
        UserController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthenticationHttpSecurityTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String EXPIRED_TOKEN = "expired-token";
    private static final String MALFORMED_TOKEN = "malformed-token";
    private static final String INVALID_SIGNATURE_TOKEN = "invalid-signature-token";
    private static final String WRONG_ISSUER_TOKEN = "wrong-issuer-token";
    private static final String WRONG_AUDIENCE_TOKEN = "wrong-audience-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private GithubService githubService;

    @MockitoBean
    private Auth0UserService auth0UserService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void configureJwtDecoder() {
        when(jwtDecoder.decode(VALID_TOKEN)).thenReturn(validJwt());
        reject(EXPIRED_TOKEN, "JWT is expired");
        reject(MALFORMED_TOKEN, "Sensitive malformed-token decoder details");
        reject(INVALID_SIGNATURE_TOKEN, "JWT signature is invalid");
        reject(WRONG_ISSUER_TOKEN, "JWT issuer is invalid");
        reject(WRONG_AUDIENCE_TOKEN, "JWT audience is invalid");
    }

    @Test
    void protectedEndpointWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required to access this resource"));

        verifyNoInteractions(userService);
    }

    @Test
    void protectedEndpointWithValidJwtReachesController() throws Exception {
        mockMvc.perform(authenticatedGet("/api/users/me", VALID_TOKEN))
                .andExpect(status().isOk());

        verify(userService).getCurrentUserProfile();
    }

    @Test
    void protectedEndpointWithExpiredJwtReturns401() throws Exception {
        expectUnauthorized(EXPIRED_TOKEN);
    }

    @Test
    void protectedEndpointWithMalformedJwtReturnsSafe401() throws Exception {
        mockMvc.perform(authenticatedGet("/api/users/me", MALFORMED_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required to access this resource"))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().string(not(containsString("Sensitive malformed-token decoder details"))));

        verifyNoInteractions(userService);
    }

    @Test
    void protectedEndpointWithInvalidSignatureReturns401() throws Exception {
        expectUnauthorized(INVALID_SIGNATURE_TOKEN);
    }

    @Test
    void protectedEndpointWithWrongIssuerReturns401() throws Exception {
        expectUnauthorized(WRONG_ISSUER_TOKEN);
    }

    @Test
    void protectedEndpointWithWrongAudienceReturns401() throws Exception {
        expectUnauthorized(WRONG_AUDIENCE_TOKEN);
    }

    @Test
    void intentionallyPublicGithubEndpointWithoutJwtReachesController() throws Exception {
        mockMvc.perform(get("/api/github/orgs/debtlens"))
                .andExpect(status().isOk());

        verify(githubService).getOrganization("debtlens");
    }

    @Test
    void githubMembershipValidationWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/github/orgs/debtlens/validate-my-membership"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(auth0UserService, githubService);
    }

    @Test
    void githubMembershipValidationWithValidJwtReachesController() throws Exception {
        when(auth0UserService.getAuthenticatedAuth0UserId())
                .thenReturn("auth0|valid-user");

        mockMvc.perform(authenticatedGet(
                        "/api/github/orgs/debtlens/validate-my-membership",
                        VALID_TOKEN
                ))
                .andExpect(status().isOk());

        verify(githubService).validateUserMembershipByAuth0UserId(
                "debtlens",
                "auth0|valid-user"
        );
    }

    private void expectUnauthorized(String token) throws Exception {
        mockMvc.perform(authenticatedGet("/api/users/me", token))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    private void reject(String token, String reason) {
        when(jwtDecoder.decode(token)).thenThrow(new BadJwtException(reason));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    authenticatedGet(String endpoint, String token) {
        return get(endpoint).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static Jwt validJwt() {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue(VALID_TOKEN)
                .header("alg", "RS256")
                .issuer("https://issuer.example.test/")
                .audience(java.util.List.of("https://api.example.test"))
                .subject("auth0|valid-user")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .build();
    }
}
