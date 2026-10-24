package com.sentinelx.backend.incidents.repository;

import com.sentinelx.backend.incidents.entity.Incident;
import com.sentinelx.backend.incidents.model.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT i FROM Incident i WHERE i.tenantId = :tenantId " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (:severity IS NULL OR i.severity = :severity) " +
            "ORDER BY i.updatedAt DESC")
    Page<Incident> searchIncidents(
            @Param("tenantId") UUID tenantId,
            @Param("status") IncidentStatus status,
            @Param("severity") String severity,
            Pageable pageable
    );
}
