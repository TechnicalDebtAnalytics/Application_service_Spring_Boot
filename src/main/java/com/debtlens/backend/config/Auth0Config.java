package com.debtlens.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth0")
public record Auth0Config(
        String domain,
        String clientId,
        String clientSecret,
        String audience,
        String databaseConnection
) {
}