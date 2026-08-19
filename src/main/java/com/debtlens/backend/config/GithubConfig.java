package com.debtlens.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public record GithubConfig(
        String token,
        String apiUrl
) {
    public GithubConfig {
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://api.github.com";
        }
    }
}
