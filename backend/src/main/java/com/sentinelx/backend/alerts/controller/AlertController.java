package com.sentinelx.backend.alerts.controller;

import com.sentinelx.backend.alerts.dto.AlertResponse;
import com.sentinelx.backend.alerts.dto.UpdateAlertRequest;
import com.sentinelx.backend.alerts.model.AlertStatus;
import com.sentinelx.backend.alerts.service.AlertService;
import com.sentinelx.backend.incidents.dto.CreateIncidentFromAlertRequest;
import com.sentinelx.backend.incidents.dto.IncidentResponse;
import com.sentinelx.backend.incidents.service.IncidentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;
    private final IncidentService incidentService;

    public AlertController(AlertService alertService, IncidentService incidentService) {
        this.alertService = alertService;
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<Page<AlertResponse>> getAlerts(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) AlertStatus status,
            @PageableDefault(size = 20, sort = "lastSeenAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(alertService.searchAlerts(severity, status, pageable));
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable UUID alertId) {
        return ResponseEntity.ok(alertService.getAlertById(alertId));
    }

    @PatchMapping("/{alertId}")
    public ResponseEntity<AlertResponse> updateAlert(
            @PathVariable UUID alertId,
            @RequestBody UpdateAlertRequest request
    ) {
        return ResponseEntity.ok(alertService.updateAlert(alertId, request.status(), request.assignedTo()));
    }

    @PostMapping("/{alertId}/incidents")
    public ResponseEntity<IncidentResponse> createIncidentFromAlert(
            @PathVariable UUID alertId,
            @RequestBody(required = false) CreateIncidentFromAlertRequest request
    ) {
        IncidentResponse created = incidentService.createIncidentFromAlert(alertId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
