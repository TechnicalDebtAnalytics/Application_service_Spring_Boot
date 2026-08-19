package com.debtlens.backend.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubRepoResponse(
        Long id,
        String name,
        @JsonProperty("full_name")
        String fullName,
        @JsonProperty("html_url")
        String htmlUrl,
        @JsonProperty("default_branch")
        String defaultBranch,
        String description,
        @JsonProperty("private")
        Boolean isPrivate,
        String language,
        @JsonProperty("stargazers_count")
        Integer stargazersCount,
        @JsonProperty("forks_count")
        Integer forksCount,
        @JsonProperty("updated_at")
        String updatedAt
) {
}
