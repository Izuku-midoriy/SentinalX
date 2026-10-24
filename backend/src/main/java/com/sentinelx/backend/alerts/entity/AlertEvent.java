package com.sentinelx.backend.alerts.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_events", indexes = {
        @Index(name = "idx_alert_events_alert_id", columnList = "alert_id"),
        @Index(name = "idx_alert_events_event_id", columnList = "event_id")
})
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private Alert alert;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AlertEvent() {}

    public AlertEvent(Alert alert, UUID eventId) {
        this.alert = alert;
        this.eventId = eventId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Alert getAlert() { return alert; }
    public void setAlert(Alert alert) { this.alert = alert; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public Instant getCreatedAt() { return createdAt; }
}
