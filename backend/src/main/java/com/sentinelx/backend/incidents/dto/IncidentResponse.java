package com.sentinelx.backend.incidents.dto;

import com.sentinelx.backend.incidents.entity.Incident;
import com.sentinelx.backend.incidents.model.IncidentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        UUID tenantId,
        String title,
        String description,
        String severity,
        IncidentStatus status,
        UUID assignedTo,
        String assignedToEmail,
        UUID createdBy,
        String createdByEmail,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        List<UUID> linkedAlertIds
) {
    public static IncidentResponse from(Incident i, List<UUID> alertIds) {
        return new IncidentResponse(
                i.getId(),
                i.getTenantId(),
                i.getTitle(),
                i.getDescription(),
                i.getSeverity(),
                i.getStatus(),
                i.getAssignedTo(),
                i.getAssignedToEmail(),
                i.getCreatedBy(),
                i.getCreatedByEmail(),
                i.getCreatedAt(),
                i.getUpdatedAt(),
                i.getResolvedAt(),
                alertIds != null ? alertIds : List.of()
        );
    }
}
