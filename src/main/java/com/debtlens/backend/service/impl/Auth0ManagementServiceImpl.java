package com.debtlens.backend.service.impl;

import com.debtlens.backend.service.Auth0ManagementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class Auth0ManagementServiceImpl
        implements Auth0ManagementService {

    @Value("${auth0.domain}")
    private String domain;

    @Value("${auth0.client-id}")
    private String clientId;

    @Value("${auth0.client-secret}")
    private String clientSecret;

    @Value("${auth0.audience}")
    private String audience;

    @Value("${auth0.database-connection}")
    private String databaseConnection;

    @Value("${auth0.roles.system-user}")
    private String roleId;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public Auth0ManagementServiceImpl(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    private String getManagementToken() {

        String tokenUrl =
                "https://" + domain + "/oauth/token";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        Map<String, String> body =
                new HashMap<>();

        body.put(
                "client_id",
                clientId
        );

        body.put(
                "client_secret",
                clientSecret
        );

        body.put(
                "audience",
                audience
        );

        body.put(
                "grant_type",
                "client_credentials"
        );

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        tokenUrl,
                        request,
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {

            throw new RuntimeException(
                    "Failed to obtain Auth0 Management API token: "
                            + response.getBody()
            );
        }

        try {

            JsonNode json =
                    objectMapper.readTree(
                            response.getBody()
                    );

            JsonNode accessToken =
                    json.get("access_token");

            if (accessToken == null ||
                    accessToken.asText().isBlank()) {

                throw new RuntimeException(
                        "Auth0 did not return an access token"
                );
            }

            return accessToken.asText();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse Auth0 token response",
                    e
            );
        }
    }

    @Override
    public String createUser(
            String email,
            String password,
            String firstName,
            String lastName
    ) {

        String managementToken =
                getManagementToken();

        String url =
                "https://" +
                        domain +
                        "/api/v2/users";

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setBearerAuth(
                managementToken
        );

        Map<String, Object> body =
                new HashMap<>();

        body.put(
                "email",
                email
        );

        body.put(
                "password",
                password
        );

        body.put(
                "connection",
                databaseConnection
        );

        body.put(
                "given_name",
                firstName
        );

        body.put(
                "family_name",
                lastName
        );

        body.put(
                "name",
                firstName + " " + lastName
        );

        body.put(
                "email_verified",
                false
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        ResponseEntity<String> response;

        try {

            response =
                    restTemplate.postForEntity(
                            url,
                            request,
                            String.class
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to create user in Auth0: "
                            + e.getMessage(),
                    e
            );
        }

        if (!response.getStatusCode().is2xxSuccessful()) {

            throw new RuntimeException(
                    "Auth0 user creation failed: "
                            + response.getBody()
            );
        }

        try {

            JsonNode json =
                    objectMapper.readTree(
                            response.getBody()
                    );

            JsonNode userId =
                    json.get("user_id");

            if (userId == null ||
                    userId.asText().isBlank()) {

                throw new RuntimeException(
                        "Auth0 did not return a user_id"
                );
            }

            return userId.asText();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse Auth0 user response",
                    e
            );
        }
    }

    @Override
    public void assignRole(
            String auth0UserId
    ) {

        String managementToken =
                getManagementToken();

        String url =
                "https://" +
                        domain +
                        "/api/v2/users/" +
                        auth0UserId +
                        "/roles";

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setBearerAuth(
                managementToken
        );

        Map<String, Object> body =
                new HashMap<>();

        body.put(
                "roles",
                new String[]{
                        roleId
                }
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        try {

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            url,
                            request,
                            String.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()) {

                throw new RuntimeException(
                        "Auth0 role assignment failed: "
                                + response.getBody()
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to assign Auth0 role: "
                            + e.getMessage(),
                    e
            );
        }
    }
}