package com.debtlens.backend.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubOrgResponse(
        Long id,
        String login,
        String name,
        String description,
        @JsonProperty("avatar_url")
        String avatarUrl,
        @JsonProperty("html_url")
        String htmlUrl,
        @JsonProperty("public_repos")
        Integer publicRepos
) {
}
