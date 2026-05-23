package com.kiettran.stix.feed.config;

import java.time.Duration;

public record JwtConfig(
    String secret,
    Duration ttl,
    String issuer
) {}
