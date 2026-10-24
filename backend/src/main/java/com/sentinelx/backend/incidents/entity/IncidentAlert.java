package com.sentinelx.backend.incidents.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_alerts", indexes = {
        @Index(name = "idx_incident_alerts_tenant", columnList = "tenant_id"),
        @Index(name = "idx_incident_alerts_incident", columnList = "incident_id"),
        @Index(name = "idx_incident_alerts_alert", columnList = "alert_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_incident_alert", columnNames = {"incident_id", "alert_id"})
})
public class IncidentAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt = Instant.now();

    public IncidentAlert() {}

    public IncidentAlert(UUID tenantId, UUID incidentId, UUID alertId) {
        this.tenantId = tenantId;
        this.incidentId = incidentId;
        this.alertId = alertId;
        this.linkedAt = Instant.now();
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

    public UUID getAlertId() {
        return alertId;
    }

    public void setAlertId(UUID alertId) {
        this.alertId = alertId;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }
}
