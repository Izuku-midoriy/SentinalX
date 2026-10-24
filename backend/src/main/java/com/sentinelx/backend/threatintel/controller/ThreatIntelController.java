package com.sentinelx.backend.threatintel.controller;

import com.sentinelx.backend.tenancy.context.TenantContext;
import com.sentinelx.backend.threatintel.dto.*;
import com.sentinelx.backend.threatintel.model.IocType;
import com.sentinelx.backend.threatintel.service.ThreatIntelService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/threat-intel")
public class ThreatIntelController {

    private final ThreatIntelService threatIntelService;

    public ThreatIntelController(ThreatIntelService threatIntelService) {
        this.threatIntelService = threatIntelService;
    }

    @GetMapping("/iocs")
    public ResponseEntity<Page<ThreatIntelIocResponse>> getIocs(
            @RequestParam(required = false) IocType type,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(threatIntelService.searchIocs(tenantId, type, search, pageable));
    }

    @PostMapping("/iocs")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ThreatIntelIocResponse> createIoc(@Valid @RequestBody CreateIocRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        ThreatIntelIocResponse created = threatIntelService.createIoc(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/stats")
    public ResponseEntity<ThreatIntelStatsResponse> getStats() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(threatIntelService.getStats(tenantId));
    }

    @PostMapping("/lookup")
    public ResponseEntity<IocLookupResponse> lookup(@Valid @RequestBody IocLookupRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(threatIntelService.lookup(tenantId, request.value()));
    }

    @DeleteMapping("/iocs/{iocId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<Void> deleteIoc(@PathVariable UUID iocId) {
        UUID tenantId = TenantContext.getTenantId();
        threatIntelService.deleteIoc(tenantId, iocId);
        return ResponseEntity.noContent().build();
    }
}
