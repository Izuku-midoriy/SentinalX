package com.sentinelx.backend.ingestion.dto;

import com.sentinelx.backend.ingestion.entity.SecurityEvent;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID tenantId,
        Instant eventTime,
        Instant ingestedAt,
        String sourceType,
        String sourceName,
        String eventType,
        String severity,
        String host,
        String sourceIp,
        String destinationIp,
        String username,
        String processName,
        Integer processId,
        String message,
        String schemaVersion,
        String idempotencyKey,
        boolean duplicate
) {
    public static EventResponse fromEntity(SecurityEvent event, boolean duplicate) {
        return new EventResponse(
                event.getId(),
                event.getTenantId(),
                event.getEventTime(),
                event.getIngestedAt(),
                event.getSourceType(),
                event.getSourceName(),
                event.getEventType(),
                event.getSeverity(),
                event.getHost(),
                event.getSourceIp(),
                event.getDestinationIp(),
                event.getUsername(),
                event.getProcessName(),
                event.getProcessId(),
                event.getMessage(),
                event.getSchemaVersion(),
                event.getIdempotencyKey(),
                duplicate
        );
    }
}
