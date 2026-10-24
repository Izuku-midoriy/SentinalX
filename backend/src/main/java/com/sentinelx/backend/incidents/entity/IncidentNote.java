package com.sentinelx.backend.incidents.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_notes", indexes = {
        @Index(name = "idx_incident_notes_tenant", columnList = "tenant_id"),
        @Index(name = "idx_incident_notes_incident", columnList = "incident_id, created_at")
})
public class IncidentNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_email", nullable = false, length = 150)
    private String authorEmail;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public IncidentNote() {}

    public IncidentNote(UUID tenantId, UUID incidentId, UUID authorId, String authorEmail, String body) {
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.authorId = authorId;
        this.authorEmail = authorEmail;
        this.body = body;
        this.createdAt = Instant.now();
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

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
