package com.sentinelx.backend.alerts.entity;

import com.sentinelx.backend.alerts.model.AlertStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_alerts_tenant_dedup", columnList = "tenant_id, deduplication_key"),
        @Index(name = "idx_alerts_tenant_last_seen", columnList = "tenant_id, last_seen_at")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_key", nullable = false, length = 100)
    private String ruleKey;

    @Column(name = "rule_version", nullable = false, length = 20)
    private String ruleVersion;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(name = "confidence")
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "event_count", nullable = false)
    private int eventCount = 1;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "deduplication_key", nullable = false, length = 255)
    private String deduplicationKey;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AlertEvent> events = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Alert() {}

    public Alert(UUID tenantId, String ruleKey, String ruleVersion, String title,
                 String description, String severity, Double confidence,
                 String deduplicationKey, Instant firstSeenAt) {
        this.tenantId = tenantId;
        this.ruleKey = ruleKey;
        this.ruleVersion = ruleVersion;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.confidence = confidence;
        this.deduplicationKey = deduplicationKey;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = firstSeenAt;
        this.eventCount = 1;
        this.status = AlertStatus.OPEN;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addEvent(UUID eventId) {
        AlertEvent alertEvent = new AlertEvent(this, eventId);
        this.events.add(alertEvent);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }
    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public void setDeduplicationKey(String deduplicationKey) { this.deduplicationKey = deduplicationKey; }
    public List<AlertEvent> getEvents() { return events; }
    public void setEvents(List<AlertEvent> events) { this.events = events; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
