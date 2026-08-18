package com.debtlens.backend.integration.auth0;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Auth0CreateUserResponse(

        @JsonProperty("user_id")
        String userId,

        String email,

        @JsonProperty("email_verified")
        boolean emailVerified
) {
}