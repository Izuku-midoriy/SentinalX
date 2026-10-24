package com.sentinelx.backend.threatintel.entity;

import com.sentinelx.backend.threatintel.model.IocType;
import com.sentinelx.backend.threatintel.model.ThreatSeverity;
import com.sentinelx.backend.threatintel.model.ThreatType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "threat_intel_iocs", indexes = {
        @Index(name = "idx_ioc_value_active", columnList = "ioc_value, active"),
        @Index(name = "idx_ioc_tenant_active", columnList = "tenant_id, active")
})
public class ThreatIntelIoc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Null indicates a global, platform-wide community feed IOC.
     * Non-null indicates a tenant-specific private custom indicator.
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ioc_type", nullable = false, length = 30)
    private IocType iocType;

    @Column(name = "ioc_value", nullable = false, length = 255)
    private String iocValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_type", nullable = false, length = 40)
    private ThreatType threatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ThreatSeverity severity = ThreatSeverity.HIGH;

    @Column(nullable = false)
    private Double confidence = 0.90;

    @Column(name = "source_feed", nullable = false, length = 100)
    private String sourceFeed = "Local Curated Blocklist";

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "match_count", nullable = false)
    private long matchCount = 0;

    @Column(name = "last_matched_at")
    private Instant lastMatchedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ThreatIntelIoc() {}

    public ThreatIntelIoc(
            UUID tenantId,
            IocType iocType,
            String iocValue,
            ThreatType threatType,
            ThreatSeverity severity,
            Double confidence,
            String sourceFeed,
            String description
    ) {
        this.tenantId = tenantId;
        this.iocType = iocType;
        this.iocValue = iocValue != null ? iocValue.trim().toLowerCase() : "";
        this.threatType = threatType;
        this.severity = severity != null ? severity : ThreatSeverity.HIGH;
        this.confidence = confidence != null ? confidence : 0.90;
        this.sourceFeed = sourceFeed != null ? sourceFeed : "Local Curated Blocklist";
        this.description = description;
        this.active = true;
        this.matchCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.iocValue != null) {
            this.iocValue = this.iocValue.trim().toLowerCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.iocValue != null) {
            this.iocValue = this.iocValue.trim().toLowerCase();
        }
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

    public IocType getIocType() {
        return iocType;
    }

    public void setIocType(IocType iocType) {
        this.iocType = iocType;
    }

    public String getIocValue() {
        return iocValue;
    }

    public void setIocValue(String iocValue) {
        this.iocValue = iocValue != null ? iocValue.trim().toLowerCase() : "";
    }

    public ThreatType getThreatType() {
        return threatType;
    }

    public void setThreatType(ThreatType threatType) {
        this.threatType = threatType;
    }

    public ThreatSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ThreatSeverity severity) {
        this.severity = severity;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getSourceFeed() {
        return sourceFeed;
    }

    public void setSourceFeed(String sourceFeed) {
        this.sourceFeed = sourceFeed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(long matchCount) {
        this.matchCount = matchCount;
    }

    public Instant getLastMatchedAt() {
        return lastMatchedAt;
    }

    public void setLastMatchedAt(Instant lastMatchedAt) {
        this.lastMatchedAt = lastMatchedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
