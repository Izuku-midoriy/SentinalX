package com.sentinelx.backend.incidents.controller;

import com.sentinelx.backend.incidents.dto.*;
import com.sentinelx.backend.incidents.model.IncidentStatus;
import com.sentinelx.backend.incidents.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        IncidentResponse created = incidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) String severity,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(incidentService.searchIncidents(status, severity, pageable));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(incidentService.getIncidentById(incidentId));
    }

    @PatchMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> updateIncident(
            @PathVariable UUID incidentId,
            @RequestBody UpdateIncidentRequest request
    ) {
        return ResponseEntity.ok(incidentService.updateIncident(incidentId, request));
    }

    @PostMapping("/{incidentId}/notes")
    public ResponseEntity<IncidentNoteResponse> addNote(
            @PathVariable UUID incidentId,
            @Valid @RequestBody AddIncidentNoteRequest request
    ) {
        IncidentNoteResponse note = incidentService.addNote(incidentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @GetMapping("/{incidentId}/notes")
    public ResponseEntity<List<IncidentNoteResponse>> getNotes(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(incidentService.getNotes(incidentId));
    }

    @GetMapping("/{incidentId}/timeline")
    public ResponseEntity<List<IncidentTimelineResponse>> getTimeline(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(incidentService.getTimeline(incidentId));
    }
}
