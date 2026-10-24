package com.sentinelx.backend.incidents.repository;

import com.sentinelx.backend.incidents.entity.IncidentAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentAlertRepository extends JpaRepository<IncidentAlert, UUID> {

    List<IncidentAlert> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);

    Optional<IncidentAlert> findByTenantIdAndIncidentIdAndAlertId(UUID tenantId, UUID incidentId, UUID alertId);

    boolean existsByTenantIdAndIncidentIdAndAlertId(UUID tenantId, UUID incidentId, UUID alertId);
}
