package com.sentinelx.backend.incidents.repository;

import com.sentinelx.backend.incidents.entity.IncidentTimelineEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentTimelineRepository extends JpaRepository<IncidentTimelineEntry, UUID> {

    List<IncidentTimelineEntry> findByTenantIdAndIncidentIdOrderByTimestampDesc(UUID tenantId, UUID incidentId);
}
