package com.sentinelx.backend.alerts.dto;

import com.sentinelx.backend.alerts.entity.Alert;
import com.sentinelx.backend.alerts.entity.AlertEvent;
import com.sentinelx.backend.alerts.model.AlertStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID tenantId,
        String ruleKey,
        String ruleVersion,
        String title,
        String description,
        String severity,
        Double confidence,
        AlertStatus status,
        Instant firstSeenAt,
        Instant lastSeenAt,
        int eventCount,
        UUID assignedTo,
        String deduplicationKey,
        List<UUID> supportingEventIds
) {
    public static AlertResponse fromEntity(Alert alert) {
        List<UUID> eventIds = alert.getEvents() != null
                ? alert.getEvents().stream().map(AlertEvent::getEventId).toList()
                : List.of();

        return new AlertResponse(
                alert.getId(),
                alert.getTenantId(),
                alert.getRuleKey(),
                alert.getRuleVersion(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getSeverity(),
                alert.getConfidence(),
                alert.getStatus(),
                alert.getFirstSeenAt(),
                alert.getLastSeenAt(),
                alert.getEventCount(),
                alert.getAssignedTo(),
                alert.getDeduplicationKey(),
                eventIds
        );
    }
}
