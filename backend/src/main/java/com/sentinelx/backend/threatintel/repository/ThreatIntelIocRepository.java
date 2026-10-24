package com.sentinelx.backend.threatintel.repository;

import com.sentinelx.backend.threatintel.entity.ThreatIntelIoc;
import com.sentinelx.backend.threatintel.model.IocType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThreatIntelIocRepository extends JpaRepository<ThreatIntelIoc, UUID> {

    @Query("SELECT i FROM ThreatIntelIoc i WHERE i.iocValue = :iocValue AND i.active = true " +
           "AND (i.tenantId IS NULL OR i.tenantId = :tenantId) " +
           "AND (i.expiresAt IS NULL OR i.expiresAt > :now) " +
           "ORDER BY CASE WHEN i.tenantId IS NOT NULL THEN 0 ELSE 1 END, i.confidence DESC")
    List<ThreatIntelIoc> findMatchingIocs(
            @Param("iocValue") String iocValue,
            @Param("tenantId") UUID tenantId,
            @Param("now") Instant now
    );

    @Query("SELECT i FROM ThreatIntelIoc i WHERE (i.tenantId IS NULL OR i.tenantId = :tenantId)")
    Page<ThreatIntelIoc> findAllForTenant(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT i FROM ThreatIntelIoc i WHERE (i.tenantId IS NULL OR i.tenantId = :tenantId) AND i.iocType = :iocType")
    Page<ThreatIntelIoc> findByTypeForTenant(@Param("tenantId") UUID tenantId, @Param("iocType") IocType iocType, Pageable pageable);

    @Query("SELECT i FROM ThreatIntelIoc i WHERE (i.tenantId IS NULL OR i.tenantId = :tenantId) " +
           "AND (LOWER(i.iocValue) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(i.sourceFeed) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ThreatIntelIoc> searchForTenant(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT i FROM ThreatIntelIoc i WHERE (i.tenantId IS NULL OR i.tenantId = :tenantId) " +
           "AND i.iocType = :iocType " +
           "AND (LOWER(i.iocValue) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(i.sourceFeed) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ThreatIntelIoc> searchByTypeForTenant(@Param("tenantId") UUID tenantId, @Param("iocType") IocType iocType, @Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(i) FROM ThreatIntelIoc i WHERE (i.tenantId IS NULL OR i.tenantId = :tenantId) AND i.active = true")
    long countActiveIocs(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(i) FROM ThreatIntelIoc i WHERE (i.tenantId IS NULL OR i.tenantId = :tenantId) AND i.active = true AND i.severity IN (com.sentinelx.backend.threatintel.model.ThreatSeverity.HIGH, com.sentinelx.backend.threatintel.model.ThreatSeverity.CRITICAL)")
    long countHighCriticalIocs(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(i.matchCount), 0) FROM ThreatIntelIoc i WHERE (i.tenantId IS NULL OR i.tenantId = :tenantId)")
    long countTotalMatches(@Param("tenantId") UUID tenantId);

    Optional<ThreatIntelIoc> findByIocValueAndTenantId(String iocValue, UUID tenantId);
}
