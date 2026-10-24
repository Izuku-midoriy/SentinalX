package com.sentinelx.backend.ai.controller;

import com.sentinelx.backend.ai.dto.AiServiceStatusResponse;
import com.sentinelx.backend.ai.dto.FlowScoreRequest;
import com.sentinelx.backend.ai.dto.FlowScoreResponse;
import com.sentinelx.backend.ai.dto.IncidentSummaryResponse;
import com.sentinelx.backend.ai.service.AiClientService;
import com.sentinelx.backend.tenancy.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiClientService aiClientService;

    public AiController(AiClientService aiClientService) {
        this.aiClientService = aiClientService;
    }

    @GetMapping("/status")
    public ResponseEntity<AiServiceStatusResponse> getAiStatus() {
        return ResponseEntity.ok(aiClientService.getStatus());
    }

    @PostMapping("/score-flow")
    public ResponseEntity<FlowScoreResponse> scoreFlow(@RequestBody(required = false) FlowScoreRequest request) {
        return ResponseEntity.ok(aiClientService.scoreFlow(request));
    }

    @PostMapping("/incidents/{incidentId}/summarize")
    public ResponseEntity<IncidentSummaryResponse> summarizeIncident(@PathVariable UUID incidentId) {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(aiClientService.summarizeIncident(tenantId, incidentId));
    }
}
