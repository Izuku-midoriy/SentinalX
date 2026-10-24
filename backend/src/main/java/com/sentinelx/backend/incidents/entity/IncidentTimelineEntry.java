package com.sentinelx.backend.incidents.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_timeline", indexes = {
        @Index(name = "idx_incident_timeline_tenant", columnList = "tenant_id"),
        @Index(name = "idx_incident_timeline_incident", columnList = "incident_id, timestamp")
})
public class IncidentTimelineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 150)
    private String actorEmail;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 255)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    public IncidentTimelineEntry() {}

    public IncidentTimelineEntry(UUID tenantId, UUID incidentId, UUID actorId, String actorEmail,
                                 String eventType, String summary, String details) {
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.eventType = eventType;
        this.summary = summary;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
