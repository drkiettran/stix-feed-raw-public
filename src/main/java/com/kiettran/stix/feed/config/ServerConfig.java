package com.kiettran.stix.feed.config;

import java.time.Duration;

public record ServerConfig(
    int port,
    int threadPoolSize,
    int backlog,
    Duration shutdownGrace
) {}
