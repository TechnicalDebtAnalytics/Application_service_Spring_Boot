package com.debtlens.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_GITHUB_CONTRIBUTORS = "github-contributors";
    public static final String CACHE_GITHUB_REPOSITORIES = "github-repos";
    public static final String CACHE_GITHUB_MEMBERS = "github-members";

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_GITHUB_CONTRIBUTORS,
                CACHE_GITHUB_REPOSITORIES,
                CACHE_GITHUB_MEMBERS
        );
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
