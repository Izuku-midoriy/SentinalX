package com.sentinelx.backend.identity.dto;

import com.sentinelx.backend.identity.model.Role;

import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String displayName,
        UUID tenantId,
        String tenantName,
        String tenantSlug,
        Role role
) {}
