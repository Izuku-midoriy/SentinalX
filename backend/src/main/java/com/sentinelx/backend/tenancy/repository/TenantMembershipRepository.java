package com.sentinelx.backend.tenancy.repository;

import com.sentinelx.backend.tenancy.entity.TenantMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    @Query("SELECT tm FROM TenantMembership tm JOIN FETCH tm.tenant WHERE tm.user.id = :userId AND tm.tenant.id = :tenantId")
    Optional<TenantMembership> findByTenantIdAndUserId(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Query("SELECT tm FROM TenantMembership tm JOIN FETCH tm.tenant WHERE tm.user.id = :userId")
    List<TenantMembership> findByUserId(@Param("userId") UUID userId);

    List<TenantMembership> findByTenantId(UUID tenantId);
}
