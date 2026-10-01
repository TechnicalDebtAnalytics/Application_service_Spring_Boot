package com.debtlens.backend.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubCommitItem(
        String sha,
        GithubCommitDetail commit,
        GithubCommitUser author,
        GithubCommitUser committer
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GithubCommitDetail(
            GithubCommitAuthorInfo author,
            GithubCommitAuthorInfo committer,
            String message
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GithubCommitAuthorInfo(
            String name,
            String email,
            String date
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GithubCommitUser(
            Long id,
            String login,
            @JsonProperty("avatar_url")
            String avatarUrl,
            @JsonProperty("html_url")
            String htmlUrl,
            String type
    ) {}
}
