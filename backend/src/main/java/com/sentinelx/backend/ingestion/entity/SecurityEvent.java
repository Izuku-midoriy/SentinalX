package com.sentinelx.backend.ingestion.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_events", indexes = {
        @Index(name = "idx_events_tenant_time", columnList = "tenant_id, event_time"),
        @Index(name = "idx_events_tenant_type", columnList = "tenant_id, event_type"),
        @Index(name = "idx_events_tenant_idempotency", columnList = "tenant_id, idempotency_key")
})
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String severity = "INFO";

    @Column(length = 100)
    private String host;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "destination_ip", length = 45)
    private String destinationIp;

    @Column(length = 100)
    private String username;

    @Column(name = "process_name", length = 100)
    private String processName;

    @Column(name = "process_id")
    private Integer processId;

    @Column(length = 1000)
    private String message;

    @Column(name = "raw_event", columnDefinition = "TEXT")
    private String rawEvent;

    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion = "1.0";

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SecurityEvent() {}

    @PrePersist
    protected void onCreate() {
        if (this.ingestedAt == null) {
            this.ingestedAt = Instant.now();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.schemaVersion == null) {
            this.schemaVersion = "1.0";
        }
        if (this.severity == null) {
            this.severity = "INFO";
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public Instant getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getDestinationIp() { return destinationIp; }
    public void setDestinationIp(String destinationIp) { this.destinationIp = destinationIp; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public Integer getProcessId() { return processId; }
    public void setProcessId(Integer processId) { this.processId = processId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRawEvent() { return rawEvent; }
    public void setRawEvent(String rawEvent) { this.rawEvent = rawEvent; }
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
