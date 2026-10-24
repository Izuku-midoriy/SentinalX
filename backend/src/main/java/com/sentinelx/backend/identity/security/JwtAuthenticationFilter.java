package com.sentinelx.backend.identity.security;

import com.sentinelx.backend.identity.model.Role;
import com.sentinelx.backend.identity.service.JwtService;
import com.sentinelx.backend.tenancy.context.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7).trim();
        try {
            Claims claims = jwtService.parseAndValidateToken(jwt);
            String userIdStr = claims.getSubject();
            String email = claims.get("email", String.class);
            String tenantIdStr = claims.get("tenantId", String.class);
            String roleStr = claims.get("role", String.class);

            if (userIdStr != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId = UUID.fromString(userIdStr);
                UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;
                Role role = roleStr != null ? Role.valueOf(roleStr) : null;

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + (role != null ? role.name() : "ANALYST"));
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email != null ? email : userIdStr,
                        null,
                        Collections.singletonList(authority)
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);

                TenantContext.set(new TenantContext.CurrentPrincipal(
                        userId,
                        tenantId,
                        role,
                        email
                ));
            }
        } catch (Exception ignored) {
            // Token is invalid/expired; clear context and proceed so SecurityFilterChain denies access
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
