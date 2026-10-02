package com.debtlens.backend.integration.github;

import com.debtlens.backend.config.CacheConfig;
import com.debtlens.backend.integration.github.dto.GithubContributorResponse;
import com.debtlens.backend.integration.github.dto.GithubRepoResponse;
import com.debtlens.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GithubServiceCacheTest {
    private GithubClient client;
    private GithubService service;
    private CacheManager caches;
    private final GithubRepoResponse repo = new GithubRepoResponse(
            1L, "backend", "acme/backend", "https://github.com/acme/backend", "main",
            "", false, "Java", 0, 0, null);
    private final GithubContributorResponse contributor = new GithubContributorResponse(
            1L, "octocat", "", "", 5, "User");

    @BeforeEach
    void setUp() {
        client = mock(GithubClient.class);
        var config = new CacheConfig();
        caches = config.cacheManager(config.caffeineConfig());
        service = new GithubService(client, mock(GithubAppTokenService.class), mock(UserRepository.class), caches);
    }

    @Test
    void overloadsAndCaseVariantsShareCachedRepositories() {
        when(client.getOrganizationRepositories("acme", null)).thenReturn(List.of(repo));
        service.getRepositories("acme");
        assertEquals(List.of(repo), service.getRepositories(" ACME ", null));
        verify(client, times(1)).getOrganizationRepositories("acme", null);
    }

    @Test
    void differentInstallationsCannotShareRepositoryOrContributorEntries() {
        when(client.getOrganizationRepositories("acme", 10L)).thenReturn(List.of(repo));
        when(client.getOrganizationRepositories("acme", 20L)).thenReturn(List.of());
        assertEquals(1, service.getRepositories("acme", 10L).size());
        assertTrue(service.getRepositories("acme", 20L).isEmpty());
        when(client.getRepoContributors("acme", "backend", 10L)).thenReturn(List.of(contributor));
        when(client.getRepoContributors("acme", "backend", 20L)).thenReturn(List.of());
        assertEquals(1, service.getContributors("acme", "backend", 10L).size());
        assertTrue(service.getContributors("acme", "backend", 20L).isEmpty());
    }

    @Test
    void membershipFallbackReusesAlreadyFetchedGitHubData() {
        when(client.getOrganizationMembers("acme", 10L)).thenReturn(List.of());
        when(client.getOrganizationRepositories("acme", 10L)).thenReturn(List.of(repo));
        when(client.getRepoContributors("acme", "backend", 10L)).thenReturn(List.of(contributor));
        service.getRepositories("acme", 10L);
        service.getContributors("acme", "backend", 10L);
        assertTrue(service.validateUserMembership("acme", "octocat", 10L).isMember());
        assertTrue(service.validateUserMembership("acme", "octocat", 10L).isMember());
        verify(client, times(1)).getOrganizationRepositories("acme", 10L);
        verify(client, times(1)).getRepoContributors("acme", "backend", 10L);
        verify(client, times(1)).getOrganizationMembers("acme", 10L);
    }

    @Test
    void failedRequestIsRetriedRatherThanCachedAsEmptyContributors() {
        var failure = new IllegalStateException("GitHub rate limit exceeded");
        when(client.getRepoContributors("acme", "backend", 10L))
                .thenThrow(failure).thenReturn(List.of(contributor));
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.getContributors("acme", "backend", 10L)));
        assertEquals(1, service.getContributors("acme", "backend", 10L).size());
        service.getContributors("acme", "backend", 10L);
        verify(client, times(2)).getRepoContributors("acme", "backend", 10L);
    }

    @Test
    void evictedEntryTriggersAnotherGitHubRead() {
        when(client.getOrganizationRepositories("acme", 10L)).thenReturn(List.of(repo));
        service.getRepositories("acme", 10L);
        caches.getCache(CacheConfig.CACHE_GITHUB_REPOSITORIES).clear();
        service.getRepositories("acme", 10L);
        verify(client, times(2)).getOrganizationRepositories("acme", 10L);
    }
}
