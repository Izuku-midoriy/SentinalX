package com.sentinelx.backend.tenancy.controller;

import com.sentinelx.backend.tenancy.context.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TenantSecurityDemoController {

    @GetMapping("/tenant/resource")
    public ResponseEntity<Map<String, Object>> getTenantScopedResource() {
        UUID tenantId = TenantContext.requireTenantId();
        return ResponseEntity.ok(Map.of(
                "tenantId", tenantId.toString(),
                "content", "Tenant-scoped confidential intelligence records for tenant " + tenantId
        ));
    }

    @GetMapping("/admin/platform-overview")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, String>> getPlatformAdminOverview() {
        return ResponseEntity.ok(Map.of(
                "message", "Authorized access to platform-wide administration metrics",
                "scope", "GLOBAL"
        ));
    }
}
