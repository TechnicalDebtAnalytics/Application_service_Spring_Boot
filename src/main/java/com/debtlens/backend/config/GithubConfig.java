package com.debtlens.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public record GithubConfig(
        String token,
        String apiUrl,
        App app
) {
    public GithubConfig {
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://api.github.com";
        }
        if (app == null) {
            app = new App(null, "debtlens", null, null, null, null);
        }
    }

    public record App(
            String id,
            String slug,
            String privateKeyPath,
            String privateKeyBase64,
            String clientId,
            String clientSecret
    ) {
        public boolean isConfigured() {
            return id != null && !id.isBlank() &&
                    ((privateKeyPath != null && !privateKeyPath.isBlank()) || (privateKeyBase64 != null && !privateKeyBase64.isBlank()));
        }
    }
}
