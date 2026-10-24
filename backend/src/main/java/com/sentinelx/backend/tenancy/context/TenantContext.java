package com.sentinelx.backend.tenancy.context;

import com.sentinelx.backend.identity.model.Role;

import java.util.UUID;

public final class TenantContext {

    public record CurrentPrincipal(
            UUID userId,
            UUID tenantId,
            Role role,
            String email
    ) {}

    private static final ThreadLocal<CurrentPrincipal> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(CurrentPrincipal principal) {
        CURRENT.set(principal);
    }

    public static CurrentPrincipal get() {
        return CURRENT.get();
    }

    public static UUID getTenantId() {
        CurrentPrincipal principal = CURRENT.get();
        return principal != null ? principal.tenantId() : null;
    }

    public static UUID getUserId() {
        CurrentPrincipal principal = CURRENT.get();
        return principal != null ? principal.userId() : null;
    }

    public static Role getRole() {
        CurrentPrincipal principal = CURRENT.get();
        return principal != null ? principal.role() : null;
    }

    public static UUID requireTenantId() {
        UUID tenantId = getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Operation requires an authenticated tenant context");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
