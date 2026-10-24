package com.sentinelx.backend.identity.dto;

import com.sentinelx.backend.identity.model.Role;

import java.util.UUID;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        String displayName,
        UUID tenantId,
        String tenantSlug,
        Role role
) {}
