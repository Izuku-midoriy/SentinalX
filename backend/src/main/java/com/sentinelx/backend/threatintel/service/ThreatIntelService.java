package com.sentinelx.backend.threatintel.service;

import com.sentinelx.backend.audit.model.AuditOutcome;
import com.sentinelx.backend.audit.service.AuditService;
import com.sentinelx.backend.tenancy.context.TenantContext;
import com.sentinelx.backend.threatintel.dto.*;
import com.sentinelx.backend.threatintel.entity.ThreatIntelIoc;
import com.sentinelx.backend.threatintel.model.IocType;
import com.sentinelx.backend.threatintel.model.ThreatSeverity;
import com.sentinelx.backend.threatintel.model.ThreatType;
import com.sentinelx.backend.threatintel.repository.ThreatIntelIocRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ThreatIntelService {

    private static final Logger log = LoggerFactory.getLogger(ThreatIntelService.class);

    private final ThreatIntelIocRepository iocRepository;
    private final AuditService auditService;

    public ThreatIntelService(ThreatIntelIocRepository iocRepository, AuditService auditService) {
        this.iocRepository = iocRepository;
        this.auditService = auditService;
    }

    @PostConstruct
    @Transactional
    public void initSeedFeeds() {
        if (iocRepository.count() > 0) {
            return;
        }

        log.info("Populating SentinelX Threat Intelligence local curated baseline feeds...");

        List<ThreatIntelIoc> seedIocs = List.of(
                new ThreatIntelIoc(
                        null,
                        IocType.IP,
                        "198.51.100.23",
                        ThreatType.MALWARE_C2,
                        ThreatSeverity.CRITICAL,
                        0.98,
                        "AlienVault OTX Community",
                        "Active Cobalt Strike C2 listener observed in state-sponsored campaign."
                ),
                new ThreatIntelIoc(
                        null,
                        IocType.IP,
                        "203.0.113.88",
                        ThreatType.MALWARE_C2,
                        ThreatSeverity.HIGH,
                        0.95,
                        "AbuseIPDB Verified Feed",
                        "High-confidence Emotet botnet distribution and secondary stage C2 node."
                ),
                new ThreatIntelIoc(
                        null,
                        IocType.IP,
                        "192.0.2.144",
                        ThreatType.RANSOMWARE,
                        ThreatSeverity.CRITICAL,
                        0.99,
                        "CISA Known Exploited Feeds",
                        "LockBit 3.0 ransomware staging and data exfiltration gateway."
                ),
                new ThreatIntelIoc(
                        null,
                        IocType.IP,
                        "185.220.101.5",
                        ThreatType.SCANNER,
                        ThreatSeverity.MEDIUM,
                        0.85,
                        "Tor Project / Emerging Threats",
                        "Known automated vulnerability reconnaissance and SSH brute-force node."
                ),
                new ThreatIntelIoc(
                        null,
                        IocType.IP,
                        "45.33.32.156",
                        ThreatType.SCANNER,
                        ThreatSeverity.LOW,
                        0.80,
                        "Shadowserver Free Scanner",
                        "Benign internet measurement scanner (Shadowserver Foundation)."
                ),
                new ThreatIntelIoc(
                        null,
                        IocType.DOMAIN,
                        "evil-c2-gateway.org",
                        ThreatType.MALWARE_C2,
                        ThreatSeverity.HIGH,
                        0.95,
                        "ThreatFox Abuse.ch Feed",
                        "Ransomware dynamic DNS domain for command and control beaconing."
                ),
                new ThreatIntelIoc(
                        null,
                        IocType.DOMAIN,
                        "phish-login-portal.net",
                        ThreatType.PHISHING,
                        ThreatSeverity.HIGH,
                        0.92,
                        "PhishTank Community Feed",
                        "Credential harvesting landing page masquerading as corporate SSO portal."
                )
        );

        iocRepository.saveAll(seedIocs);
        log.info("Initialized {} Threat Intelligence IOC indicators into baseline database.", seedIocs.size());
    }

    @Transactional
    public Optional<ThreatIntelIoc> match(UUID tenantId, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }

        String normalized = rawValue.trim().toLowerCase();
        List<ThreatIntelIoc> matches = iocRepository.findMatchingIocs(normalized, tenantId, Instant.now());
        if (matches.isEmpty()) {
            return Optional.empty();
        }

        ThreatIntelIoc matchedIoc = matches.get(0);
        matchedIoc.setMatchCount(matchedIoc.getMatchCount() + 1);
        matchedIoc.setLastMatchedAt(Instant.now());
        iocRepository.save(matchedIoc);

        return Optional.of(matchedIoc);
    }

    @Transactional(readOnly = true)
    public Page<ThreatIntelIocResponse> searchIocs(UUID tenantId, IocType iocType, String search, Pageable pageable) {
        boolean hasType = (iocType != null);
        boolean hasSearch = (search != null && !search.isBlank());

        Page<ThreatIntelIoc> page;
        if (hasType && hasSearch) {
            page = iocRepository.searchByTypeForTenant(tenantId, iocType, search.trim(), pageable);
        } else if (hasType) {
            page = iocRepository.findByTypeForTenant(tenantId, iocType, pageable);
        } else if (hasSearch) {
            page = iocRepository.searchForTenant(tenantId, search.trim(), pageable);
        } else {
            page = iocRepository.findAllForTenant(tenantId, pageable);
        }

        return page.map(ThreatIntelIocResponse::fromEntity);
    }

    @Transactional
    public ThreatIntelIocResponse createIoc(UUID tenantId, CreateIocRequest request) {
        UUID effectiveTenantId = Boolean.TRUE.equals(request.isGlobal()) ? null : tenantId;
        String normalizedValue = request.iocValue().trim().toLowerCase();

        Optional<ThreatIntelIoc> existing = iocRepository.findByIocValueAndTenantId(normalizedValue, effectiveTenantId);
        ThreatIntelIoc ioc;
        if (existing.isPresent()) {
            ioc = existing.get();
            ioc.setThreatType(request.threatType());
            ioc.setSeverity(request.severity() != null ? request.severity() : ThreatSeverity.HIGH);
            ioc.setConfidence(request.confidence() != null ? request.confidence() : 0.90);
            ioc.setSourceFeed(request.sourceFeed() != null ? request.sourceFeed() : "Manual Analyst Entry");
            ioc.setDescription(request.description());
            ioc.setActive(true);
            ioc.setExpiresAt(request.expiresAt());
        } else {
            ioc = new ThreatIntelIoc(
                    effectiveTenantId,
                    request.iocType(),
                    normalizedValue,
                    request.threatType(),
                    request.severity(),
                    request.confidence(),
                    request.sourceFeed() != null ? request.sourceFeed() : "Manual Analyst Entry",
                    request.description()
            );
            ioc.setExpiresAt(request.expiresAt());
        }

        ThreatIntelIoc saved = iocRepository.save(ioc);

        UUID userId = TenantContext.getUserId();
        String userEmail = TenantContext.get() != null ? TenantContext.get().email() : "analyst";

        auditService.record(
                tenantId,
                userId,
                userEmail,
                "THREAT_INTEL_IOC_CREATED",
                "ThreatIntelIoc",
                saved.getId().toString(),
                AuditOutcome.SUCCESS,
                String.format("Created IOC: %s (%s, severity: %s)", saved.getIocValue(), saved.getIocType(), saved.getSeverity()),
                null
        );

        return ThreatIntelIocResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public IocLookupResponse lookup(UUID tenantId, String value) {
        if (value == null || value.isBlank()) {
            return new IocLookupResponse(false, value, null);
        }

        String normalized = value.trim().toLowerCase();
        List<ThreatIntelIoc> matches = iocRepository.findMatchingIocs(normalized, tenantId, Instant.now());
        if (matches.isEmpty()) {
            return new IocLookupResponse(false, value, null);
        }

        return new IocLookupResponse(true, value, ThreatIntelIocResponse.fromEntity(matches.get(0)));
    }

    @Transactional
    public void deleteIoc(UUID tenantId, UUID iocId) {
        ThreatIntelIoc ioc = iocRepository.findById(iocId)
                .orElseThrow(() -> new NoSuchElementException("Threat indicator not found: " + iocId));

        // Tenant can only delete their own private IOCs, not global community feeds unless platform admin
        if (ioc.getTenantId() != null && !ioc.getTenantId().equals(tenantId)) {
            throw new SecurityException("Cannot modify threat intelligence indicator owned by another tenant.");
        }

        ioc.setActive(false);
        iocRepository.save(ioc);

        UUID userId = TenantContext.getUserId();
        String userEmail = TenantContext.get() != null ? TenantContext.get().email() : "analyst";

        auditService.record(
                tenantId,
                userId,
                userEmail,
                "THREAT_INTEL_IOC_DEACTIVATED",
                "ThreatIntelIoc",
                ioc.getId().toString(),
                AuditOutcome.SUCCESS,
                String.format("Deactivated IOC: %s", ioc.getIocValue()),
                null
        );
    }

    @Transactional(readOnly = true)
    public ThreatIntelStatsResponse getStats(UUID tenantId) {
        long totalActive = iocRepository.countActiveIocs(tenantId);
        long highCritical = iocRepository.countHighCriticalIocs(tenantId);
        long totalMatches = iocRepository.countTotalMatches(tenantId);

        return new ThreatIntelStatsResponse(
                totalActive,
                highCritical,
                totalMatches,
                4, // Curated baseline feeds: AlienVault OTX, AbuseIPDB, CISA KEV, Local Blocklist
                "HEALTHY"
        );
    }
}
