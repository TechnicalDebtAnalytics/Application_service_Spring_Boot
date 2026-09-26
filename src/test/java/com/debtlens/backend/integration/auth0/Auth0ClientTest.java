package com.debtlens.backend.integration.auth0;

import com.debtlens.backend.config.Auth0Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Auth0ClientTest {

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec postSpec;
    private RestClient.RequestHeadersUriSpec<?> getSpec;
    private RestClient.ResponseSpec postResponse;
    private RestClient.ResponseSpec getResponse;
    private Auth0Client client;

    @BeforeEach
    void setUp() {
        Auth0Config config = new Auth0Config(
                "tenant.example.test",
                "client-id",
                "client-secret",
                "https://tenant.example.test/api/v2/",
                "Username-Password-Authentication"
        );
        client = new Auth0Client(config);
        restClient = mock(RestClient.class);
        postSpec = mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
        getSpec = mock(RestClient.RequestHeadersUriSpec.class, RETURNS_SELF);
        postResponse = mock(RestClient.ResponseSpec.class);
        getResponse = mock(RestClient.ResponseSpec.class);
        when(restClient.post()).thenReturn(postSpec);
        doReturn(getSpec).when(restClient).get();
        when(postSpec.retrieve()).thenReturn(postResponse);
        when(getSpec.retrieve()).thenReturn(getResponse);
        ReflectionTestUtils.setField(client, "restClient", restClient);
    }

    @Test
    void getManagementApiToken_shouldReturnAccessToken() {
        when(postResponse.body(Auth0TokenResponse.class))
                .thenReturn(new Auth0TokenResponse("management-token", "Bearer", 3600));

        assertEquals("management-token", client.getManagementApiToken());
        verify(postSpec).uri("/oauth/token");
    }

    @Test
    void getManagementApiToken_shouldRejectNullResponse() {
        when(postResponse.body(Auth0TokenResponse.class)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, client::getManagementApiToken);

        assertEquals("Failed to obtain Auth0 Management API token", exception.getMessage());
    }

    @Test
    void getManagementApiToken_shouldPropagateHttpFailure() {
        when(postResponse.body(Auth0TokenResponse.class))
                .thenThrow(new ResourceAccessException("Auth0 unavailable"));

        ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                client::getManagementApiToken
        );

        assertEquals("Auth0 unavailable", exception.getMessage());
    }

    @Test
    void createUser_shouldReturnAuth0Response() {
        Auth0CreateUserResponse expected = new Auth0CreateUserResponse("auth0|123", "ada@example.com", false);
        when(postResponse.body(Auth0CreateUserResponse.class)).thenReturn(expected);

        Auth0CreateUserResponse actual = client.createUser(
                "management-token", "ada@example.com", "password123", "Ada", "Lovelace"
        );

        assertSame(expected, actual);
        verify(postSpec).uri("/api/v2/users");
        verify(postSpec).header("Authorization", "Bearer management-token");
        verify(postSpec).body(any(Auth0CreateUserRequest.class));
    }

    @Test
    void createUser_shouldPropagateHttpFailure() {
        when(postResponse.body(Auth0CreateUserResponse.class))
                .thenThrow(new ResourceAccessException("create failed"));

        ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                () -> client.createUser("token", "ada@example.com", "password123", "Ada", "Lovelace")
        );

        assertEquals("create failed", exception.getMessage());
    }

    @Test
    void assignRole_shouldPostRoleRequest() {
        when(postResponse.toBodilessEntity())
                .thenReturn(ResponseEntity.noContent().build());

        client.assignRole("management-token", "auth0|123", "role-admin");

        verify(postSpec).uri("/api/v2/users/{userId}/roles", "auth0|123");
        verify(postSpec).header("Authorization", "Bearer management-token");
        verify(postSpec).body(any(Auth0RoleRequest.class));
        verify(postResponse).toBodilessEntity();
    }

    @Test
    void assignRole_shouldPropagateHttpFailure() {
        when(postResponse.toBodilessEntity())
                .thenThrow(new ResourceAccessException("assign failed"));

        ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                () -> client.assignRole("token", "auth0|123", "role-admin")
        );

        assertEquals("assign failed", exception.getMessage());
    }

    @Test
    void getUserRoles_shouldReturnRoles() {
        List<Auth0RoleResponse> expected = List.of(new Auth0RoleResponse("role-admin", "ADMIN", null));
        when(getResponse.body(any(ParameterizedTypeReference.class))).thenReturn(expected);

        assertSame(expected, client.getUserRoles("management-token", "auth0|123"));
        verify(getSpec).uri("/api/v2/users/{userId}/roles", "auth0|123");
        verify(getSpec).header("Authorization", "Bearer management-token");
    }

    @Test
    void getUserRoles_shouldConvertNullResponseToEmptyList() {
        when(getResponse.body(any(ParameterizedTypeReference.class))).thenReturn(null);

        assertTrue(client.getUserRoles("token", "auth0|123").isEmpty());
    }

    @Test
    void getUserRoles_shouldPropagateHttpFailure() {
        when(getResponse.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("roles failed"));

        ResourceAccessException exception = assertThrows(
                ResourceAccessException.class,
                () -> client.getUserRoles("token", "auth0|123")
        );

        assertEquals("roles failed", exception.getMessage());
    }
}
