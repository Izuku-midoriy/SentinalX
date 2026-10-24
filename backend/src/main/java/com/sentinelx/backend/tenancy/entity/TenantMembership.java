package com.sentinelx.backend.tenancy.entity;

import com.sentinelx.backend.identity.entity.User;
import com.sentinelx.backend.identity.model.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_memberships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_user_membership", columnNames = {"tenant_id", "user_id"})
}, indexes = {
        @Index(name = "idx_membership_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_membership_user_id", columnList = "user_id")
})
public class TenantMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TenantMembership() {}

    public TenantMembership(Tenant tenant, User user, Role role) {
        this.tenant = tenant;
        this.user = user;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Instant getCreatedAt() { return createdAt; }
}
