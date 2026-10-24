package com.sentinelx.backend.alerts.repository;

import com.sentinelx.backend.alerts.entity.Alert;
import com.sentinelx.backend.alerts.model.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @Query("SELECT a FROM Alert a LEFT JOIN FETCH a.events WHERE a.tenantId = :tenantId AND a.id = :id")
    Optional<Alert> findByTenantIdAndIdWithEvents(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    Optional<Alert> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT a FROM Alert a LEFT JOIN FETCH a.events WHERE a.tenantId = :tenantId AND a.deduplicationKey = :deduplicationKey AND a.status IN :statuses")
    Optional<Alert> findByTenantIdAndDeduplicationKeyAndStatusIn(
            @Param("tenantId") UUID tenantId,
            @Param("deduplicationKey") String deduplicationKey,
            @Param("statuses") List<AlertStatus> statuses
    );

    @Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId " +
           "AND (:severity IS NULL OR a.severity = :severity) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "ORDER BY a.lastSeenAt DESC")
    Page<Alert> searchAlerts(
            @Param("tenantId") UUID tenantId,
            @Param("severity") String severity,
            @Param("status") AlertStatus status,
            Pageable pageable
    );

    long countByTenantIdAndStatus(UUID tenantId, AlertStatus status);
    long countByTenantIdAndSeverity(UUID tenantId, String severity);
}
