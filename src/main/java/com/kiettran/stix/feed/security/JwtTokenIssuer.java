package com.kiettran.stix.feed.security;

import com.kiettran.stix.feed.config.JwtConfig;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Mints HS256 JWTs. */
public final class JwtTokenIssuer {

    private final JwtConfig config;
    private final SecretKey key;

    public JwtTokenIssuer(JwtConfig config) {
        this.config = config;
        byte[] keyBytes = config.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least 32 bytes for HS256 (got " + keyBytes.length + ")"
            );
        }
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String issue(String subject, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(config.ttl());
        return Jwts.builder()
            .subject(subject)
            .issuer(config.issuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claims(Map.of("roles", roles))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }

    public long ttlSeconds() { return config.ttl().toSeconds(); }
}
