package com.kiettran.stix.feed.config;

public record KafkaConfig(
    String bootstrapServers,
    String topic,
    int requestTimeoutMs,
    int deliveryTimeoutMs
) {}
