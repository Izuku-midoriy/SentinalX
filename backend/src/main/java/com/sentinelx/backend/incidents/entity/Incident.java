package com.sentinelx.backend.incidents.entity;

import com.sentinelx.backend.incidents.model.IncidentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents", indexes = {
        @Index(name = "idx_incidents_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_incidents_tenant_severity", columnList = "tenant_id, severity"),
        @Index(name = "idx_incidents_tenant_updated", columnList = "tenant_id, updated_at")
})
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_to_email", length = 150)
    private String assignedToEmail;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_by_email", length = 150)
    private String createdByEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Incident() {}

    public Incident(UUID tenantId, String title, String description, String severity,
                    UUID createdBy, String createdByEmail) {
        this.tenantId = tenantId;
        this.title = title;
        this.description = description;
        this.severity = severity != null ? severity.toUpperCase().trim() : "HIGH";
        this.status = IncidentStatus.OPEN;
        this.createdBy = createdBy;
        this.createdByEmail = createdByEmail;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity != null ? severity.toUpperCase().trim() : this.severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
        if (status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED) {
            if (this.resolvedAt == null) {
                this.resolvedAt = Instant.now();
            }
        } else {
            this.resolvedAt = null;
        }
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getAssignedToEmail() {
        return assignedToEmail;
    }

    public void setAssignedToEmail(String assignedToEmail) {
        this.assignedToEmail = assignedToEmail;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
