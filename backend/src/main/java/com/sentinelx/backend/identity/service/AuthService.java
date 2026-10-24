package com.sentinelx.backend.identity.service;

import com.sentinelx.backend.audit.model.AuditOutcome;
import com.sentinelx.backend.audit.service.AuditService;
import com.sentinelx.backend.identity.dto.LoginRequest;
import com.sentinelx.backend.identity.dto.LoginResponse;
import com.sentinelx.backend.identity.dto.RegisterRequest;
import com.sentinelx.backend.identity.dto.UserProfileResponse;
import com.sentinelx.backend.identity.entity.User;
import com.sentinelx.backend.identity.model.Role;
import com.sentinelx.backend.identity.model.UserStatus;
import com.sentinelx.backend.identity.repository.UserRepository;
import com.sentinelx.backend.tenancy.context.TenantContext;
import com.sentinelx.backend.tenancy.entity.Tenant;
import com.sentinelx.backend.tenancy.entity.TenantMembership;
import com.sentinelx.backend.tenancy.repository.TenantMembershipRepository;
import com.sentinelx.backend.tenancy.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final long expirationMs;

    public AuthService(
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String normalizedEmail = request.email().toLowerCase().trim();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())
                || user.getStatus() != UserStatus.ACTIVE) {
            auditService.record(
                    null,
                    user != null ? user.getId() : null,
                    normalizedEmail,
                    "AUTH_LOGIN_FAILURE",
                    "USER",
                    user != null ? user.getId().toString() : "UNKNOWN",
                    AuditOutcome.FAILURE,
                    "Invalid credentials provided",
                    ipAddress
            );
            throw new BadCredentialsException("Invalid email or password");
        }

        List<TenantMembership> memberships = membershipRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            auditService.record(
                    null,
                    user.getId(),
                    user.getEmail(),
                    "AUTH_LOGIN_FAILURE",
                    "USER",
                    user.getId().toString(),
                    AuditOutcome.FAILURE,
                    "User has no assigned organization memberships",
                    ipAddress
            );
            throw new BadCredentialsException("Account has no active organization membership");
        }

        // Active primary membership
        TenantMembership primaryMembership = memberships.get(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user, primaryMembership.getTenant().getId(), primaryMembership.getRole());

        auditService.record(
                primaryMembership.getTenant().getId(),
                user.getId(),
                user.getEmail(),
                "AUTH_LOGIN_SUCCESS",
                "USER",
                user.getId().toString(),
                AuditOutcome.SUCCESS,
                "User logged in successfully with role " + primaryMembership.getRole().name(),
                ipAddress
        );

        return new LoginResponse(
                token,
                "Bearer",
                expirationMs / 1000,
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                primaryMembership.getTenant().getId(),
                primaryMembership.getTenant().getSlug(),
                primaryMembership.getRole()
        );
    }

    @Transactional
    public LoginResponse register(RegisterRequest request, String ipAddress) {
        String normalizedEmail = request.email().toLowerCase().trim();
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("An account with email " + normalizedEmail + " already exists");
        }

        String targetSlug = (request.tenantSlug() != null && !request.tenantSlug().isBlank())
                ? request.tenantSlug().trim()
                : "alpha-lab";

        Tenant tenant = tenantRepository.findBySlug(targetSlug)
                .orElseThrow(() -> new IllegalArgumentException("Tenant organization not found with slug: " + targetSlug));

        Role assignedRole = Role.ANALYST;
        if (request.role() != null && !request.role().isBlank()) {
            try {
                assignedRole = Role.valueOf(request.role().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Default to ANALYST if invalid
            }
        }

        User newUser = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                UserStatus.ACTIVE
        );
        newUser.setLastLoginAt(Instant.now());
        User savedUser = userRepository.save(newUser);

        TenantMembership membership = new TenantMembership(tenant, savedUser, assignedRole);
        membershipRepository.save(membership);

        String token = jwtService.generateToken(savedUser, tenant.getId(), assignedRole);

        auditService.record(
                tenant.getId(),
                savedUser.getId(),
                savedUser.getEmail(),
                "AUTH_USER_REGISTERED",
                "USER",
                savedUser.getId().toString(),
                AuditOutcome.SUCCESS,
                "New user registered: " + savedUser.getDisplayName() + " (" + assignedRole.name() + ") in " + tenant.getName(),
                ipAddress
        );

        return new LoginResponse(
                token,
                "Bearer",
                expirationMs / 1000,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getDisplayName(),
                tenant.getId(),
                tenant.getSlug(),
                assignedRole
        );
    }

    public void logout(String ipAddress) {
        TenantContext.CurrentPrincipal principal = TenantContext.get();
        if (principal != null) {
            auditService.record(
                    principal.tenantId(),
                    principal.userId(),
                    principal.email(),
                    "AUTH_LOGOUT",
                    "USER",
                    principal.userId().toString(),
                    AuditOutcome.SUCCESS,
                    "User logged out",
                    ipAddress
            );
        }
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        TenantContext.CurrentPrincipal principal = TenantContext.get();
        if (principal == null) {
            throw new BadCredentialsException("Unauthenticated context");
        }

        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        TenantMembership membership = membershipRepository
                .findByTenantIdAndUserId(principal.tenantId(), principal.userId())
                .orElseThrow(() -> new BadCredentialsException("Active tenant membership not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                membership.getTenant().getId(),
                membership.getTenant().getName(),
                membership.getTenant().getSlug(),
                membership.getRole()
        );
    }
}
