package com.debtlens.backend.integration.auth0;

import java.util.List;

public record Auth0RoleRequest(
        List<String> roles
) {
}