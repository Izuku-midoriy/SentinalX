package com.sentinelx.backend.incidents.dto;

import com.sentinelx.backend.incidents.entity.IncidentNote;

import java.time.Instant;
import java.util.UUID;

public record IncidentNoteResponse(
        UUID id,
        UUID incidentId,
        UUID authorId,
        String authorEmail,
        String body,
        Instant createdAt
) {
    public static IncidentNoteResponse from(IncidentNote note) {
        return new IncidentNoteResponse(
                note.getId(),
                note.getIncidentId(),
                note.getAuthorId(),
                note.getAuthorEmail(),
                note.getBody(),
                note.getCreatedAt()
        );
    }
}
