package com.debtlens.backend.integration.auth0;

import com.debtlens.backend.config.Auth0Config;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class Auth0Client {

    private final Auth0Config auth0Config;
    private final RestClient restClient;

    public Auth0Client(Auth0Config auth0Config) {

        this.auth0Config = auth0Config;

        this.restClient = RestClient.builder()
                .baseUrl("https://" + auth0Config.domain())
                .build();
    }

    public String getManagementApiToken() {

        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add("grant_type", "client_credentials");
        formData.add("client_id", auth0Config.clientId());
        formData.add("client_secret", auth0Config.clientSecret());
        formData.add("audience", auth0Config.audience());

        Auth0TokenResponse response = restClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Auth0TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new RuntimeException(
                    "Failed to obtain Auth0 Management API token"
            );
        }

        return response.accessToken();
    }

    public Auth0CreateUserResponse createUser(
            String managementToken,
            String email,
            String password,
            String firstName,
            String lastName
    ) {

        Auth0CreateUserRequest request =
                new Auth0CreateUserRequest(
                        auth0Config.databaseConnection(),
                        email,
                        password,
                        firstName,
                        lastName,
                        firstName + " " + lastName
                );

        return restClient.post()
                .uri("/api/v2/users")
                .header(
                        "Authorization",
                        "Bearer " + managementToken
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Auth0CreateUserResponse.class);
    }

    public void assignRole(
            String managementToken,
            String userId,
            String roleId
    ) {

        Auth0RoleRequest request =
                new Auth0RoleRequest(
                        List.of(roleId)
                );

        restClient.post()
                .uri("/api/v2/users/{userId}/roles", userId)
                .header(
                        "Authorization",
                        "Bearer " + managementToken
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public List<Auth0RoleResponse> getUserRoles(
            String managementToken,
            String userId
    ) {
        List<Auth0RoleResponse> roles = restClient.get()
                .uri("/api/v2/users/{userId}/roles", userId)
                .header(
                        "Authorization",
                        "Bearer " + managementToken
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<Auth0RoleResponse>>() {});

        return roles != null ? roles : List.of();
    }
}