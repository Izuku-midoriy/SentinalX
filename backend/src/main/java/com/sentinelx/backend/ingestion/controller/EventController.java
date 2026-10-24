package com.sentinelx.backend.ingestion.controller;

import com.sentinelx.backend.ingestion.dto.BatchEventIngestRequest;
import com.sentinelx.backend.ingestion.dto.BatchEventIngestResponse;
import com.sentinelx.backend.ingestion.dto.EventIngestRequest;
import com.sentinelx.backend.ingestion.dto.EventResponse;
import com.sentinelx.backend.ingestion.service.EventIngestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventIngestionService ingestionService;

    public EventController(EventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> ingestEvent(@Valid @RequestBody EventIngestRequest request) {
        EventResponse response = ingestionService.ingestEvent(request);
        HttpStatus status = response.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchEventIngestResponse> ingestBatch(@Valid @RequestBody BatchEventIngestRequest request) {
        BatchEventIngestResponse response = ingestionService.ingestBatch(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> searchEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by("eventTime").descending());
        Page<EventResponse> events = ingestionService.searchEvents(eventType, severity, sourceType, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID eventId) {
        EventResponse event = ingestionService.getEventById(eventId);
        return ResponseEntity.ok(event);
    }
}
