package com.sentinelx.backend.incidents.service;

import com.sentinelx.backend.alerts.entity.Alert;
import com.sentinelx.backend.alerts.repository.AlertRepository;
import com.sentinelx.backend.audit.model.AuditOutcome;
import com.sentinelx.backend.audit.service.AuditService;
import com.sentinelx.backend.identity.repository.UserRepository;
import com.sentinelx.backend.incidents.dto.*;
import com.sentinelx.backend.incidents.entity.Incident;
import com.sentinelx.backend.incidents.entity.IncidentAlert;
import com.sentinelx.backend.incidents.entity.IncidentNote;
import com.sentinelx.backend.incidents.entity.IncidentTimelineEntry;
import com.sentinelx.backend.incidents.model.IncidentStatus;
import com.sentinelx.backend.incidents.repository.IncidentAlertRepository;
import com.sentinelx.backend.incidents.repository.IncidentNoteRepository;
import com.sentinelx.backend.incidents.repository.IncidentRepository;
import com.sentinelx.backend.incidents.repository.IncidentTimelineRepository;
import com.sentinelx.backend.tenancy.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentAlertRepository incidentAlertRepository;
    private final IncidentNoteRepository incidentNoteRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final com.sentinelx.backend.operations.metrics.SentinelXMetrics metrics;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentAlertRepository incidentAlertRepository,
            IncidentNoteRepository incidentNoteRepository,
            IncidentTimelineRepository incidentTimelineRepository,
            AlertRepository alertRepository,
            UserRepository userRepository,
            AuditService auditService,
            com.sentinelx.backend.operations.metrics.SentinelXMetrics metrics
    ) {
        this.incidentRepository = incidentRepository;
        this.incidentAlertRepository = incidentAlertRepository;
        this.incidentNoteRepository = incidentNoteRepository;
        this.incidentTimelineRepository = incidentTimelineRepository;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.getUserId();
        String userEmail = getUserEmail();

        Incident incident = new Incident(
                tenantId,
                request.title(),
                request.description(),
                request.severity(),
                userId != null ? userId : UUID.randomUUID(),
                userEmail
        );

        if (request.assignedToEmail() != null && !request.assignedToEmail().isBlank()) {
            String targetEmail = request.assignedToEmail().trim().toLowerCase();
            incident.setAssignedToEmail(targetEmail);
            userRepository.findByEmailIgnoreCase(targetEmail).ifPresent(u -> incident.setAssignedTo(u.getId()));
        } else if (request.assignedTo() != null) {
            incident.setAssignedTo(request.assignedTo());
            userRepository.findById(request.assignedTo()).ifPresent(u -> incident.setAssignedToEmail(u.getEmail()));
        }

        Incident saved = incidentRepository.save(incident);
        metrics.recordIncidentCreated(saved.getSeverity());

        // Link alerts if provided
        List<UUID> linkedAlertIds = new ArrayList<>();
        if (request.alertIds() != null) {
            for (UUID alertId : request.alertIds()) {
                if (alertRepository.findByTenantIdAndId(tenantId, alertId).isPresent()) {
                    incidentAlertRepository.save(new IncidentAlert(tenantId, saved.getId(), alertId));
                    linkedAlertIds.add(alertId);
                }
            }
        }

        // Timeline: CREATED
        incidentTimelineRepository.save(new IncidentTimelineEntry(
                tenantId,
                saved.getId(),
                userId,
                userEmail,
                "INCIDENT_CREATED",
                "Incident opened by " + userEmail,
                "Title: " + saved.getTitle() + " | Severity: " + saved.getSeverity()
        ));

        // Audit
        auditService.record(
                tenantId,
                userId,
                userEmail,
                "INCIDENT_CREATED",
                "Incident",
                saved.getId().toString(),
                AuditOutcome.SUCCESS,
                "Created incident: " + saved.getTitle(),
                null
        );

        log.info("Incident created: id={}, tenant={}, title={}", saved.getId(), tenantId, saved.getTitle());
        return IncidentResponse.from(saved, linkedAlertIds);
    }

    @Transactional
    public IncidentResponse createIncidentFromAlert(UUID alertId, CreateIncidentFromAlertRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.getUserId();
        String userEmail = getUserEmail();

        Alert alert = alertRepository.findByTenantIdAndId(tenantId, alertId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found with ID: " + alertId));

        String title = (request != null && request.title() != null && !request.title().isBlank())
                ? request.title()
                : "Incident for: " + alert.getTitle();

        String description = (request != null && request.description() != null && !request.description().isBlank())
                ? request.description()
                : alert.getDescription();

        String severity = (request != null && request.severity() != null && !request.severity().isBlank())
                ? request.severity()
                : alert.getSeverity();

        Incident incident = new Incident(
                tenantId,
                title,
                description,
                severity,
                userId != null ? userId : UUID.randomUUID(),
                userEmail
        );

        if (request != null && request.assignedTo() != null) {
            incident.setAssignedTo(request.assignedTo());
            incident.setAssignedToEmail(request.assignedToEmail());
        } else if (alert.getAssignedTo() != null) {
            incident.setAssignedTo(alert.getAssignedTo());
        }

        Incident saved = incidentRepository.save(incident);
        metrics.recordIncidentCreated(saved.getSeverity());

        // Link Alert
        incidentAlertRepository.save(new IncidentAlert(tenantId, saved.getId(), alert.getId()));

        // Timeline: CREATED_FROM_ALERT
        incidentTimelineRepository.save(new IncidentTimelineEntry(
                tenantId,
                saved.getId(),
                userId,
                userEmail,
                "ALERT_ESCALATED",
                "Escalated from Alert: " + alert.getTitle(),
                "Alert ID: " + alert.getId() + " | Rule: " + alert.getRuleKey()
        ));

        // Audit
        auditService.record(
                tenantId,
                userId,
                userEmail,
                "INCIDENT_CREATED_FROM_ALERT",
                "Incident",
                saved.getId().toString(),
                AuditOutcome.SUCCESS,
                "Escalated alert " + alert.getId() + " to incident " + saved.getId(),
                null
        );

        return IncidentResponse.from(saved, List.of(alert.getId()));
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> searchIncidents(IncidentStatus status, String severity, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        String normalizedSeverity = severity != null && !severity.isBlank() ? severity.toUpperCase().trim() : null;

        return incidentRepository.searchIncidents(tenantId, status, normalizedSeverity, pageable)
                .map(i -> {
                    List<UUID> alertIds = incidentAlertRepository.findByTenantIdAndIncidentId(tenantId, i.getId())
                            .stream().map(IncidentAlert::getAlertId).toList();
                    return IncidentResponse.from(i, alertIds);
                });
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(UUID incidentId) {
        UUID tenantId = TenantContext.requireTenantId();
        Incident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found with ID: " + incidentId));

        List<UUID> alertIds = incidentAlertRepository.findByTenantIdAndIncidentId(tenantId, incident.getId())
                .stream().map(IncidentAlert::getAlertId).toList();

        return IncidentResponse.from(incident, alertIds);
    }

    @Transactional
    public IncidentResponse updateIncident(UUID incidentId, UpdateIncidentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.getUserId();
        String userEmail = getUserEmail();

        Incident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found with ID: " + incidentId));

        boolean statusChanged = false;
        IncidentStatus oldStatus = incident.getStatus();
        if (request.status() != null && request.status() != oldStatus) {
            incident.setStatus(request.status());
            statusChanged = true;
        }

        boolean assignedChanged = false;
        if (request.assignedToEmail() != null && !request.assignedToEmail().isBlank()) {
            String targetEmail = request.assignedToEmail().trim().toLowerCase();
            if (!targetEmail.equalsIgnoreCase(incident.getAssignedToEmail())) {
                incident.setAssignedToEmail(targetEmail);
                userRepository.findByEmailIgnoreCase(targetEmail).ifPresent(u -> incident.setAssignedTo(u.getId()));
                assignedChanged = true;
            }
        } else if (request.assignedTo() != null && !request.assignedTo().equals(incident.getAssignedTo())) {
            incident.setAssignedTo(request.assignedTo());
            userRepository.findById(request.assignedTo()).ifPresent(u -> incident.setAssignedToEmail(u.getEmail()));
            assignedChanged = true;
        }

        if (request.severity() != null && !request.severity().isBlank()) {
            incident.setSeverity(request.severity());
        }

        if (request.title() != null && !request.title().isBlank()) {
            incident.setTitle(request.title());
        }

        if (request.description() != null) {
            incident.setDescription(request.description());
        }

        incident.setUpdatedAt(Instant.now());
        Incident saved = incidentRepository.save(incident);

        // Timeline entries
        if (statusChanged) {
            incidentTimelineRepository.save(new IncidentTimelineEntry(
                    tenantId,
                    saved.getId(),
                    userId,
                    userEmail,
                    "STATUS_CHANGE",
                    "Status changed from " + oldStatus + " to " + saved.getStatus(),
                    null
            ));
        }

        if (assignedChanged) {
            incidentTimelineRepository.save(new IncidentTimelineEntry(
                    tenantId,
                    saved.getId(),
                    userId,
                    userEmail,
                    "ASSIGNMENT_CHANGE",
                    "Assigned to " + (saved.getAssignedToEmail() != null ? saved.getAssignedToEmail() : saved.getAssignedTo()),
                    null
            ));
        }

        // Audit
        auditService.record(
                tenantId,
                userId,
                userEmail,
                "INCIDENT_UPDATED",
                "Incident",
                saved.getId().toString(),
                AuditOutcome.SUCCESS,
                "Updated incident " + saved.getId() + " [status=" + saved.getStatus() + ", assignedTo=" + saved.getAssignedToEmail() + "]",
                null
        );

        List<UUID> alertIds = incidentAlertRepository.findByTenantIdAndIncidentId(tenantId, saved.getId())
                .stream().map(IncidentAlert::getAlertId).toList();

        return IncidentResponse.from(saved, alertIds);
    }

    @Transactional
    public IncidentNoteResponse addNote(UUID incidentId, AddIncidentNoteRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.getUserId();
        String userEmail = getUserEmail();

        Incident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found with ID: " + incidentId));

        IncidentNote note = new IncidentNote(
                tenantId,
                incident.getId(),
                userId != null ? userId : UUID.randomUUID(),
                userEmail,
                request.body()
        );

        IncidentNote savedNote = incidentNoteRepository.save(note);

        // Update incident updated_at
        incident.setUpdatedAt(Instant.now());
        incidentRepository.save(incident);

        // Timeline
        incidentTimelineRepository.save(new IncidentTimelineEntry(
                tenantId,
                incident.getId(),
                userId,
                userEmail,
                "NOTE_ADDED",
                "Note added by " + userEmail,
                request.body().length() > 100 ? request.body().substring(0, 100) + "..." : request.body()
        ));

        // Audit
        auditService.record(
                tenantId,
                userId,
                userEmail,
                "INCIDENT_NOTE_ADDED",
                "IncidentNote",
                savedNote.getId().toString(),
                AuditOutcome.SUCCESS,
                "Added note to incident " + incident.getId(),
                null
        );

        return IncidentNoteResponse.from(savedNote);
    }

    @Transactional(readOnly = true)
    public List<IncidentNoteResponse> getNotes(UUID incidentId) {
        UUID tenantId = TenantContext.requireTenantId();
        // Verify incident exists in tenant
        incidentRepository.findByTenantIdAndId(tenantId, incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found with ID: " + incidentId));

        return incidentNoteRepository.findByTenantIdAndIncidentIdOrderByCreatedAtDesc(tenantId, incidentId)
                .stream().map(IncidentNoteResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentTimelineResponse> getTimeline(UUID incidentId) {
        UUID tenantId = TenantContext.requireTenantId();
        // Verify incident exists in tenant
        incidentRepository.findByTenantIdAndId(tenantId, incidentId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found with ID: " + incidentId));

        return incidentTimelineRepository.findByTenantIdAndIncidentIdOrderByTimestampDesc(tenantId, incidentId)
                .stream().map(IncidentTimelineResponse::from).toList();
    }

    private String getUserEmail() {
        var principal = TenantContext.get();
        return principal != null && principal.email() != null ? principal.email() : "system@sentinelx.local";
    }
}
