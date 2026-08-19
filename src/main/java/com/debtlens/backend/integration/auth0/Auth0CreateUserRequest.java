package com.debtlens.backend.integration.auth0;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Auth0CreateUserRequest(

        String connection,

        String email,

        String password,

        @JsonProperty("given_name")
        String firstName,

        @JsonProperty("family_name")
        String lastName,

        String name
) {
}