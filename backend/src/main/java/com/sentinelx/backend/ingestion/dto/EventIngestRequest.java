package com.sentinelx.backend.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EventIngestRequest(
        @NotNull(message = "Timestamp is required")
        Instant timestamp,

        @NotBlank(message = "sourceType is required")
        @Size(max = 50, message = "sourceType must not exceed 50 characters")
        String sourceType,

        @NotBlank(message = "sourceName is required")
        @Size(max = 100, message = "sourceName must not exceed 100 characters")
        String sourceName,

        @NotBlank(message = "eventType is required")
        @Size(max = 100, message = "eventType must not exceed 100 characters")
        String eventType,

        @Size(max = 20, message = "severity must not exceed 20 characters")
        String severity,

        @Size(max = 100, message = "host must not exceed 100 characters")
        String host,

        @Size(max = 45, message = "sourceIp must not exceed 45 characters")
        String sourceIp,

        @Size(max = 45, message = "destinationIp must not exceed 45 characters")
        String destinationIp,

        @Size(max = 100, message = "username must not exceed 100 characters")
        String username,

        @Size(max = 100, message = "processName must not exceed 100 characters")
        String processName,

        Integer processId,

        @Size(max = 1000, message = "message must not exceed 1000 characters")
        String message,

        @Size(max = 10000, message = "rawEvent must not exceed 10000 characters")
        String rawEvent,

        @Size(max = 100, message = "idempotencyKey must not exceed 100 characters")
        String idempotencyKey
) {}
