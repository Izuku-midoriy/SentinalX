package com.sentinelx.backend.threatintel.dto;

import com.sentinelx.backend.threatintel.entity.ThreatIntelIoc;
import com.sentinelx.backend.threatintel.model.IocType;
import com.sentinelx.backend.threatintel.model.ThreatSeverity;
import com.sentinelx.backend.threatintel.model.ThreatType;

import java.time.Instant;
import java.util.UUID;

public record ThreatIntelIocResponse(
        UUID id,
        UUID tenantId,
        boolean isGlobal,
        IocType iocType,
        String iocValue,
        ThreatType threatType,
        ThreatSeverity severity,
        Double confidence,
        String sourceFeed,
        String description,
        boolean active,
        long matchCount,
        Instant lastMatchedAt,
        Instant expiresAt,
        Instant createdAt
) {
    public static ThreatIntelIocResponse fromEntity(ThreatIntelIoc entity) {
        return new ThreatIntelIocResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getTenantId() == null,
                entity.getIocType(),
                entity.getIocValue(),
                entity.getThreatType(),
                entity.getSeverity(),
                entity.getConfidence(),
                entity.getSourceFeed(),
                entity.getDescription(),
                entity.isActive(),
                entity.getMatchCount(),
                entity.getLastMatchedAt(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }
}
