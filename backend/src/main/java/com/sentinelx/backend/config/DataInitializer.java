package com.sentinelx.backend.config;

import com.sentinelx.backend.identity.entity.User;
import com.sentinelx.backend.identity.model.Role;
import com.sentinelx.backend.identity.model.UserStatus;
import com.sentinelx.backend.identity.repository.UserRepository;
import com.sentinelx.backend.tenancy.entity.Tenant;
import com.sentinelx.backend.tenancy.entity.TenantMembership;
import com.sentinelx.backend.tenancy.model.TenantStatus;
import com.sentinelx.backend.tenancy.repository.TenantMembershipRepository;
import com.sentinelx.backend.tenancy.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!prod")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            TenantMembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (tenantRepository.count() > 0) {
            return;
        }

        log.info("Initializing SentinelX development fixtures for local multi-tenant testing...");

        // Tenant Alpha
        Tenant alpha = tenantRepository.save(new Tenant("Alpha Defense Lab", "alpha-lab", TenantStatus.ACTIVE));

        // Tenant Beta (used to test cross-tenant isolation)
        Tenant beta = tenantRepository.save(new Tenant("Beta Security Ops", "beta-ops", TenantStatus.ACTIVE));

        // Users
        User analystAlpha = userRepository.save(new User(
                "analyst@alpha.local",
                passwordEncoder.encode("DevAnalyst123!"),
                "Alex Analyst",
                UserStatus.ACTIVE
        ));

        User adminAlpha = userRepository.save(new User(
                "admin@alpha.local",
                passwordEncoder.encode("DevAdmin123!"),
                "Arthur Admin",
                UserStatus.ACTIVE
        ));

        User platformAdmin = userRepository.save(new User(
                "platform@sentinelx.local",
                passwordEncoder.encode("DevPlatform123!"),
                "Patricia Platform",
                UserStatus.ACTIVE
        ));

        User analystBeta = userRepository.save(new User(
                "analyst@beta.local",
                passwordEncoder.encode("DevBetaAnalyst123!"),
                "Bob Beta Analyst",
                UserStatus.ACTIVE
        ));

        // Memberships
        membershipRepository.save(new TenantMembership(alpha, analystAlpha, Role.ANALYST));
        membershipRepository.save(new TenantMembership(alpha, adminAlpha, Role.TENANT_ADMIN));
        membershipRepository.save(new TenantMembership(alpha, platformAdmin, Role.PLATFORM_ADMIN));
        membershipRepository.save(new TenantMembership(beta, analystBeta, Role.ANALYST));

        log.info("Development fixtures loaded: 2 tenants, 4 accounts with BCrypt-hashed credentials.");
    }
}
