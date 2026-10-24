package com.sentinelx.backend.incidents.dto;

import com.sentinelx.backend.incidents.entity.IncidentTimelineEntry;

import java.time.Instant;
import java.util.UUID;

public record IncidentTimelineResponse(
        UUID id,
        UUID incidentId,
        UUID actorId,
        String actorEmail,
        String eventType,
        String summary,
        String details,
        Instant timestamp
) {
    public static IncidentTimelineResponse from(IncidentTimelineEntry entry) {
        return new IncidentTimelineResponse(
                entry.getId(),
                entry.getIncidentId(),
                entry.getActorId(),
                entry.getActorEmail(),
                entry.getEventType(),
                entry.getSummary(),
                entry.getDetails(),
                entry.getTimestamp()
        );
    }
}
