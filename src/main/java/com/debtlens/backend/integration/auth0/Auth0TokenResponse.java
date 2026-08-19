package com.debtlens.backend.integration.auth0;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Auth0TokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn
) {
}