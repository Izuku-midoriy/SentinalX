package com.sentinelx.backend.audit.service;

import com.sentinelx.backend.audit.entity.AuditLog;
import com.sentinelx.backend.audit.model.AuditOutcome;
import com.sentinelx.backend.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(UUID tenantId, UUID actorId, String actorEmail,
                           String action, String resourceType, String resourceId,
                           AuditOutcome outcome, String details, String ipAddress) {
        // Redact any accidental credential hints in details
        String safeDetails = redactSensitiveInfo(details);

        AuditLog auditLog = new AuditLog(
                tenantId,
                actorId,
                actorEmail,
                action,
                resourceType,
                resourceId,
                outcome,
                safeDetails,
                ipAddress
        );

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit event recorded: action={}, outcome={}, actor={}, tenant={}",
                action, outcome, actorEmail, tenantId);
        return saved;
    }

    private String redactSensitiveInfo(String input) {
        if (input == null) return null;
        return input.replaceAll("(?i)(password|token|secret|authorization)=[^,&}]+", "$1=[REDACTED]");
    }
}
