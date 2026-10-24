package com.sentinelx.backend.incidents.repository;

import com.sentinelx.backend.incidents.entity.IncidentNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentNoteRepository extends JpaRepository<IncidentNote, UUID> {

    List<IncidentNote> findByTenantIdAndIncidentIdOrderByCreatedAtDesc(UUID tenantId, UUID incidentId);
}
