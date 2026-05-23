package com.kiettran.stix.feed.security;

import com.kiettran.stix.feed.config.JwtConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtRoundTripTest {

    private final JwtConfig config = new JwtConfig(
        "this-is-a-32-byte-secret-for-test!", Duration.ofMinutes(5), "test"
    );

    @Test
    void issuedTokenVerifiesWithSameSecret() {
        JwtTokenIssuer issuer     = new JwtTokenIssuer(config);
        JwtTokenVerifier verifier = new JwtTokenVerifier(config);

        String token = issuer.issue("analyst", List.of("analyst", "reader"));

        Optional<JwtTokenVerifier.Principal> p = verifier.verify(token);
        assertTrue(p.isPresent());
        assertEquals("analyst", p.get().subject());
        assertTrue(p.get().hasRole("analyst"));
        assertFalse(p.get().hasRole("admin"));
    }

    @Test
    void verificationFailsWithWrongSecret() {
        JwtTokenIssuer issuer = new JwtTokenIssuer(config);
        String token = issuer.issue("analyst", List.of("analyst"));

        JwtConfig wrongCfg = new JwtConfig(
            "another-32-byte-secret-for-tests!", Duration.ofMinutes(5), "test");
        JwtTokenVerifier verifier = new JwtTokenVerifier(wrongCfg);

        assertTrue(verifier.verify(token).isEmpty());
    }
}
