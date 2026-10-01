package com.debtlens.backend.integration.github;

import com.debtlens.backend.config.GithubConfig;
import com.debtlens.backend.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.Jwts;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GithubAppTokenService {

    private static final Logger log = LoggerFactory.getLogger(GithubAppTokenService.class);

    private final GithubConfig githubConfig;
    private final RestClient restClient;
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    // Cache: installationId -> CachedToken
    private final Map<Long, CachedToken> tokenCache = new ConcurrentHashMap<>();
    private volatile PrivateKey cachedPrivateKey;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    public GithubAppTokenService(GithubConfig githubConfig) {
        this.githubConfig = githubConfig;
        this.restClient = RestClient.builder()
                .baseUrl(githubConfig.apiUrl())
                .defaultHeader("User-Agent", "DebtLens-App")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    /**
     * Check if GitHub App authentication is configured in the environment.
     */
    public boolean isConfigured() {
        return githubConfig.app() != null && githubConfig.app().isConfigured();
    }

    /**
     * Get the configured GitHub App slug (name).
     */
    public String getAppSlug() {
        if (githubConfig.app() != null && githubConfig.app().slug() != null && !githubConfig.app().slug().isBlank()) {
            return githubConfig.app().slug();
        }
        return "debtlens";
    }

    /**
     * Get the direct installation URL for users to install the GitHub App to their orgs/accounts.
     */
    public String getInstallUrl() {
        return "https://github.com/apps/" + getAppSlug() + "/installations/new";
    }

    /**
     * Obtain a valid Installation Access Token for a specific GitHub App installation ID.
     * Uses in-memory caching with proactive refresh before expiration.
     */
    public String getInstallationAccessToken(Long installationId) {
        if (installationId == null) {
            return null;
        }

        if (!isConfigured()) {
            log.warn("GitHub App is not configured, cannot generate installation token for installationId: {}", installationId);
            return null;
        }

        CachedToken cached = tokenCache.get(installationId);
        Instant now = Instant.now();
        // Return cached token if valid for at least 2 more minutes
        if (cached != null && cached.expiresAt().isAfter(now.plusSeconds(120))) {
            return cached.token();
        }

        synchronized (tokenCache) {
            cached = tokenCache.get(installationId);
            if (cached != null && cached.expiresAt().isAfter(now.plusSeconds(120))) {
                return cached.token();
            }

            try {
                String appJwt = generateAppJwt();
                InstallationTokenResponse response = restClient.post()
                        .uri("/app/installations/{installation_id}/access_tokens", installationId)
                        .header("Authorization", "Bearer " + appJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(InstallationTokenResponse.class);

                if (response != null && response.token() != null) {
                    Instant expiresAt = response.expiresAt() != null
                            ? Instant.parse(response.expiresAt())
                            : now.plusSeconds(3300); // default ~55 mins

                    tokenCache.put(installationId, new CachedToken(response.token(), expiresAt));
                    log.info("Successfully minted and cached new GitHub App installation token for installationId: {}", installationId);
                    return response.token();
                } else {
                    throw new IllegalStateException("Empty token response received from GitHub for installationId: " + installationId);
                }
            } catch (Exception ex) {
                log.error("Failed to fetch installation access token for installationId {}: {}", installationId, ex.getMessage(), ex);
                throw new BadRequestException("Failed to authenticate with GitHub App installation: " + ex.getMessage());
            }
        }
    }

    /**
     * Generates a signed RS256 JWT using the GitHub App ID and RSA Private Key.
     * Valid for 9 minutes (GitHub allows max 10 minutes).
     */
    public String generateAppJwt() {
        PrivateKey privateKey = getPrivateKey();
        if (privateKey == null) {
            throw new IllegalStateException("GitHub App RSA Private Key could not be loaded");
        }

        String appId = githubConfig.app().id().trim();
        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(appId)
                .issuedAt(Date.from(now.minusSeconds(60))) // clock drift tolerance
                .expiration(Date.from(now.plusSeconds(540))) // 9 minutes
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private PrivateKey getPrivateKey() {
        if (cachedPrivateKey != null) {
            return cachedPrivateKey;
        }

        synchronized (this) {
            if (cachedPrivateKey != null) {
                return cachedPrivateKey;
            }

            try {
                String pemContent = null;
                // 1. Try base64-encoded private key first (ideal for cloud/container env vars)
                String base64Key = githubConfig.app().privateKeyBase64();
                if (base64Key != null && !base64Key.isBlank()) {
                    String cleanBase64 = base64Key.trim();
                    if (!cleanBase64.startsWith("-----BEGIN")) {
                        byte[] decoded = Base64.getDecoder().decode(cleanBase64);
                        pemContent = new String(decoded, StandardCharsets.UTF_8);
                    } else {
                        pemContent = cleanBase64;
                    }
                }

                // 2. Fall back to file/classpath path
                if (pemContent == null || pemContent.isBlank()) {
                    String keyPath = githubConfig.app().privateKeyPath();
                    if (keyPath != null && !keyPath.isBlank()) {
                        Resource resource = resourceLoader.getResource(keyPath.trim());
                        try (InputStream is = resource.getInputStream()) {
                            pemContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    }
                }

                if (pemContent == null || pemContent.isBlank()) {
                    throw new IllegalStateException("Neither GITHUB_APP_PRIVATE_KEY_BASE64 nor GITHUB_APP_PRIVATE_KEY_PATH is configured");
                }

                cachedPrivateKey = parsePemPrivateKey(pemContent);
                log.info("GitHub App RSA Private Key loaded successfully.");
                return cachedPrivateKey;
            } catch (Exception ex) {
                log.error("Failed to load GitHub App private key: {}", ex.getMessage(), ex);
                throw new IllegalStateException("Could not parse GitHub App RSA Private Key: " + ex.getMessage(), ex);
            }
        }
    }

    private PrivateKey parsePemPrivateKey(String pemContent) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(pemContent))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (object instanceof PEMKeyPair pemKeyPair) {
                return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
            } else if (object instanceof PrivateKeyInfo privateKeyInfo) {
                return converter.getPrivateKey(privateKeyInfo);
            } else if (object != null) {
                throw new IllegalArgumentException("Unsupported PEM object type: " + object.getClass().getName());
            } else {
                throw new IllegalArgumentException("PEM parser returned null. Check PEM key formatting.");
            }
        }
    }

    private record CachedToken(String token, Instant expiresAt) {}

    public record InstallationTokenResponse(
            String token,
            @JsonProperty("expires_at") String expiresAt,
            Map<String, Object> permissions
    ) {}
}
