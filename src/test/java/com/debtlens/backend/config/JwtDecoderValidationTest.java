package com.debtlens.backend.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtDecoderValidationTest {

    private static final String ISSUER = "https://issuer.example.test/";
    private static final String AUDIENCE = "https://api.example.test";

    private KeyPair signingKey;
    private NimbusJwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        signingKey = generateKeyPair();
        decoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) signingKey.getPublic())
                .build();
        decoder.setJwtValidator(SecurityConfig.jwtValidator(
                ISSUER,
                List.of(AUDIENCE)
        ));
    }

    @Test
    void validSignedTokenIsAccepted() throws Exception {
        assertDoesNotThrow(() -> decoder.decode(token(
                signingKey,
                ISSUER,
                AUDIENCE,
                "auth0|valid-user",
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(300),
                Instant.now().minusSeconds(30)
        )));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        assertThrows(JwtException.class, () -> decoder.decode(token(
                signingKey,
                ISSUER,
                AUDIENCE,
                "auth0|expired-user",
                Instant.now().minusSeconds(600),
                Instant.now().minusSeconds(300),
                Instant.now().minusSeconds(600)
        )));
    }

    @Test
    void notBeforeTokenIsRejected() throws Exception {
        assertThrows(JwtException.class, () -> decoder.decode(token(
                signingKey,
                ISSUER,
                AUDIENCE,
                "auth0|future-user",
                Instant.now(),
                Instant.now().plusSeconds(600),
                Instant.now().plusSeconds(300)
        )));
    }

    @Test
    void invalidSignatureIsRejected() throws Exception {
        KeyPair untrustedKey = generateKeyPair();

        assertThrows(JwtException.class, () -> decoder.decode(token(
                untrustedKey,
                ISSUER,
                AUDIENCE,
                "auth0|untrusted-user",
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(300),
                Instant.now().minusSeconds(30)
        )));
    }

    @Test
    void wrongIssuerIsRejected() throws Exception {
        assertThrows(JwtException.class, () -> decoder.decode(token(
                signingKey,
                "https://wrong-issuer.example.test/",
                AUDIENCE,
                "auth0|wrong-issuer-user",
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(300),
                Instant.now().minusSeconds(30)
        )));
    }

    @Test
    void wrongAudienceIsRejected() throws Exception {
        assertThrows(JwtException.class, () -> decoder.decode(token(
                signingKey,
                ISSUER,
                "https://wrong-audience.example.test",
                "auth0|wrong-audience-user",
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(300),
                Instant.now().minusSeconds(30)
        )));
    }

    @Test
    void missingSubjectIsRejected() throws Exception {
        assertThrows(JwtException.class, () -> decoder.decode(token(
                signingKey,
                ISSUER,
                AUDIENCE,
                null,
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(300),
                Instant.now().minusSeconds(30)
        )));
    }

    @Test
    void malformedTokenIsRejected() {
        assertThrows(JwtException.class, () -> decoder.decode("not-a-jwt"));
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String token(
            KeyPair keyPair,
            String issuer,
            String audience,
            String subject,
            Instant issuedAt,
            Instant expiresAt,
            Instant notBefore
    ) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .notBeforeTime(Date.from(notBefore));

        if (subject != null) {
            claims.subject(subject);
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
                claims.build()
        );
        jwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        return jwt.serialize();
    }
}
