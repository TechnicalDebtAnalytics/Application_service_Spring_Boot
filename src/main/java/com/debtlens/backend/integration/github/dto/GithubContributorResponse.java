package com.debtlens.backend.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubContributorResponse(
        Long id,
        String login,
        @JsonProperty("avatar_url")
        String avatarUrl,
        @JsonProperty("html_url")
        String htmlUrl,
        Integer contributions,
        String type
) {
}
