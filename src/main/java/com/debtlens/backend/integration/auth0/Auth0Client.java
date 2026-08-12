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

    public Auth0UserResponse createUser(
            String managementToken,
            String email,
            String password
    ) {

        Auth0CreateUserRequest request =
                new Auth0CreateUserRequest(
                        auth0Config.databaseConnection(),
                        email,
                        password
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
                .body(Auth0UserResponse.class);
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
}