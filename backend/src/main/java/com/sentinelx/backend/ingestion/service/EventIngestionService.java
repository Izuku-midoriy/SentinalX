package com.sentinelx.backend.ingestion.service;

import com.sentinelx.backend.alerts.service.AlertService;
import com.sentinelx.backend.detection.engine.AlertCandidate;
import com.sentinelx.backend.detection.engine.DetectionEngine;
import com.sentinelx.backend.ingestion.dto.BatchEventIngestRequest;
import com.sentinelx.backend.ingestion.dto.BatchEventIngestResponse;
import com.sentinelx.backend.ingestion.dto.EventIngestRequest;
import com.sentinelx.backend.ingestion.dto.EventResponse;
import com.sentinelx.backend.ingestion.entity.SecurityEvent;
import com.sentinelx.backend.ingestion.repository.SecurityEventRepository;
import com.sentinelx.backend.tenancy.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EventIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EventIngestionService.class);

    private final SecurityEventRepository eventRepository;
    private final DetectionEngine detectionEngine;
    private final AlertService alertService;
    private final com.sentinelx.backend.operations.metrics.SentinelXMetrics metrics;

    public EventIngestionService(
            SecurityEventRepository eventRepository,
            DetectionEngine detectionEngine,
            AlertService alertService,
            com.sentinelx.backend.operations.metrics.SentinelXMetrics metrics
    ) {
        this.eventRepository = eventRepository;
        this.detectionEngine = detectionEngine;
        this.alertService = alertService;
        this.metrics = metrics;
    }

    @Transactional
    public EventResponse ingestEvent(EventIngestRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // Validate timestamp sanity: cannot be more than 1 day in the future
        if (request.timestamp().isAfter(Instant.now().plus(1, ChronoUnit.DAYS))) {
            throw new IllegalArgumentException("Event timestamp cannot be more than 24 hours in the future");
        }

        // Idempotency check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            var existing = eventRepository.findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey().trim());
            if (existing.isPresent()) {
                log.debug("Duplicate event ignored for tenant {} with idempotency key {}",
                        tenantId, request.idempotencyKey());
                metrics.recordEventDuplicate();
                return EventResponse.fromEntity(existing.get(), true);
            }
        }

        // Normalization
        SecurityEvent event = new SecurityEvent();
        event.setTenantId(tenantId);
        event.setEventTime(request.timestamp());
        event.setIngestedAt(Instant.now());
        event.setSourceType(request.sourceType().toLowerCase().trim());
        event.setSourceName(request.sourceName().trim());
        event.setEventType(request.eventType().toLowerCase().trim());
        event.setSeverity(normalizeSeverity(request.severity()));
        event.setHost(request.host());
        event.setSourceIp(request.sourceIp());
        event.setDestinationIp(request.destinationIp());
        event.setUsername(request.username());
        event.setProcessName(request.processName());
        event.setProcessId(request.processId());
        event.setMessage(request.message());
        event.setRawEvent(request.rawEvent());
        event.setSchemaVersion("1.0");
        event.setIdempotencyKey(request.idempotencyKey() != null ? request.idempotencyKey().trim() : null);

        SecurityEvent saved = eventRepository.saveAndFlush(event);
        metrics.recordEventIngested(saved.getEventType(), saved.getSeverity(), saved.getSourceType());
        log.info("Event ingested: id={}, type={}, tenant={}", saved.getId(), saved.getEventType(), tenantId);

        // Real-time detection engine evaluation & alert generation
        List<AlertCandidate> candidates = detectionEngine.evaluateEvent(saved);
        for (AlertCandidate candidate : candidates) {
            alertService.createOrUpdateAlert(tenantId, candidate);
        }

        return EventResponse.fromEntity(saved, false);
    }

    @Transactional
    public BatchEventIngestResponse ingestBatch(BatchEventIngestRequest batchRequest) {
        List<UUID> eventIds = new ArrayList<>();
        int accepted = 0;
        int duplicates = 0;

        for (EventIngestRequest req : batchRequest.events()) {
            EventResponse res = ingestEvent(req);
            eventIds.add(res.id());
            if (res.duplicate()) {
                duplicates++;
            } else {
                accepted++;
            }
        }

        return new BatchEventIngestResponse(
                batchRequest.events().size(),
                accepted,
                duplicates,
                eventIds
        );
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> searchEvents(String eventType, String severity, String sourceType, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        String normalizedType = eventType != null && !eventType.isBlank() ? eventType.toLowerCase().trim() : null;
        String normalizedSeverity = severity != null && !severity.isBlank() ? severity.toUpperCase().trim() : null;
        String normalizedSource = sourceType != null && !sourceType.isBlank() ? sourceType.toLowerCase().trim() : null;

        return eventRepository.searchEvents(tenantId, normalizedType, normalizedSeverity, normalizedSource, pageable)
                .map(e -> EventResponse.fromEntity(e, false));
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID eventId) {
        UUID tenantId = TenantContext.requireTenantId();
        SecurityEvent event = eventRepository.findByTenantIdAndId(tenantId, eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + eventId));
        return EventResponse.fromEntity(event, false);
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "INFO";
        }
        String upper = severity.toUpperCase().trim();
        return switch (upper) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> upper;
            default -> "INFO";
        };
    }
}
