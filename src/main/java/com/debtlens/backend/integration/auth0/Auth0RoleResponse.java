package com.debtlens.backend.integration.auth0;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Auth0RoleResponse(
        String id,
        String name,
        String description
) {
}
