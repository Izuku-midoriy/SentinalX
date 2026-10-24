package com.sentinelx.backend.audit.repository;

import com.sentinelx.backend.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<AuditLog> findByActorEmailOrderByCreatedAtDesc(String actorEmail);
}
