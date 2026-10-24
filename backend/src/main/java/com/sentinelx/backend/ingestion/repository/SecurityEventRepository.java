package com.sentinelx.backend.ingestion.repository;

import com.sentinelx.backend.ingestion.entity.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    Optional<SecurityEvent> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<SecurityEvent> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    @Query("SELECT e FROM SecurityEvent e WHERE e.tenantId = :tenantId " +
           "AND (:eventType IS NULL OR e.eventType = :eventType) " +
           "AND (:severity IS NULL OR e.severity = :severity) " +
           "AND (:sourceType IS NULL OR e.sourceType = :sourceType) " +
           "ORDER BY e.eventTime DESC")
    Page<SecurityEvent> searchEvents(
            @Param("tenantId") UUID tenantId,
            @Param("eventType") String eventType,
            @Param("severity") String severity,
            @Param("sourceType") String sourceType,
            Pageable pageable
    );

    @Query("SELECT e FROM SecurityEvent e WHERE e.tenantId = :tenantId " +
           "AND e.eventType = :eventType " +
           "AND (:sourceIp IS NULL OR e.sourceIp = :sourceIp) " +
           "AND (:username IS NULL OR e.username = :username) " +
           "AND e.eventTime >= :sinceTime " +
           "ORDER BY e.eventTime DESC")
    java.util.List<SecurityEvent> findRecentMatchingEvents(
            @Param("tenantId") UUID tenantId,
            @Param("eventType") String eventType,
            @Param("sourceIp") String sourceIp,
            @Param("username") String username,
            @Param("sinceTime") java.time.Instant sinceTime
    );

    @Query("SELECT e FROM SecurityEvent e WHERE e.tenantId = :tenantId " +
           "AND e.sourceIp = :sourceIp " +
           "AND e.eventTime >= :sinceTime " +
           "ORDER BY e.eventTime DESC")
    java.util.List<SecurityEvent> findRecentEventsBySourceIp(
            @Param("tenantId") UUID tenantId,
            @Param("sourceIp") String sourceIp,
            @Param("sinceTime") java.time.Instant sinceTime
    );
}
