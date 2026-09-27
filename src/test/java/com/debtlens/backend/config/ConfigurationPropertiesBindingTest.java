package com.debtlens.backend.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ConfigurationPropertiesBindingTest {

    @Test
    void cfg04_syntheticValuesBindToApplicationConfigurationProperties() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "auth0.domain", "tenant.example.test",
                "auth0.client-id", "synthetic-client-id",
                "auth0.client-secret", "synthetic-client-secret",
                "auth0.audience", "https://api.example.test",
                "auth0.roles.system-user", "synthetic-system-user-role",
                "github.token", "synthetic-github-token",
                "github.api-url", "https://github.example.test"));
        Binder binder = new Binder(source);

        Auth0Config auth0 = binder.bind("auth0", Bindable.of(Auth0Config.class))
                .orElseThrow(() -> new AssertionError("auth0 properties did not bind"));
        Auth0RoleConfig roles = binder.bind("auth0.roles", Bindable.of(Auth0RoleConfig.class))
                .orElseThrow(() -> new AssertionError("auth0 role properties did not bind"));
        GithubConfig github = binder.bind("github", Bindable.of(GithubConfig.class))
                .orElseThrow(() -> new AssertionError("GitHub properties did not bind"));

        assertAll(
                () -> assertEquals("tenant.example.test", auth0.domain()),
                () -> assertEquals("synthetic-client-id", auth0.clientId()),
                () -> assertEquals("synthetic-client-secret", auth0.clientSecret()),
                () -> assertEquals("https://api.example.test", auth0.audience()),
                () -> assertEquals("synthetic-system-user-role", roles.getSystemUser()),
                () -> assertEquals("synthetic-github-token", github.token()),
                () -> assertEquals("https://github.example.test", github.apiUrl()));
    }
}
