package com.kiettran.stix.feed.security;

import com.kiettran.stix.feed.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Verifies HS256 JWTs and extracts a {@link Principal} carrying the subject
 * and roles claims.
 *
 * Symmetric (HS256) verification is paired with {@link JwtTokenIssuer},
 * which signs with the same shared secret. Production deployments would
 * typically swap to RS256 with a JWKS endpoint, at which point this class
 * becomes the natural extension point.
 *
 * Verification failures (invalid signature, expired token, malformed JWS)
 * return {@link Optional#empty()} rather than throwing. Token-rejection is
 * an expected outcome of unauthenticated traffic, not an exceptional
 * condition; callers can handle it as a value without try/catch boilerplate.
 */
public final class JwtTokenVerifier {

    public record Principal(String subject, List<String> roles) {
        public boolean hasRole(String role) { return roles != null && roles.contains(role); }
    }

    private final SecretKey key;

    public JwtTokenVerifier(JwtConfig config) {
        byte[] keyBytes = config.secret().getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @SuppressWarnings("unchecked")
    public Optional<Principal> verify(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            Claims claims = jws.getPayload();
            Object rolesObj = claims.get("roles");
            List<String> roles = (rolesObj instanceof List<?> l)
                ? l.stream().map(String::valueOf).toList()
                : List.of();
            return Optional.of(new Principal(claims.getSubject(), roles));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
