package com.sentinelx.backend.operations.dto;

import java.time.Instant;

public record HealthResponse(
        String status,
        String service,
        String version,
        Instant timestamp,
        String environment
) {
    public static HealthResponse up(String environment) {
        return new HealthResponse(
                "UP",
                "SentinelX SOC Platform API",
                "0.1.0",
                Instant.now(),
                environment
        );
    }
}
