package com.sentinelx.backend.detection.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "detection_rules", indexes = {
        @Index(name = "idx_rules_rule_key", columnList = "rule_key", unique = true)
})
public class DetectionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_key", nullable = false, unique = true, length = 100)
    private String ruleKey;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, length = 20)
    private String severity = "MEDIUM";

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType = "THRESHOLD";

    @Column(columnDefinition = "TEXT")
    private String configuration;

    @Column(nullable = false, length = 20)
    private String version = "1.0.0";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DetectionRule() {}

    public DetectionRule(String ruleKey, String name, String description, String severity, String ruleType, String version) {
        this.ruleKey = ruleKey;
        this.name = name;
        this.description = description;
        this.severity = severity;
        this.ruleType = ruleType;
        this.version = version;
        this.enabled = true;
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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
