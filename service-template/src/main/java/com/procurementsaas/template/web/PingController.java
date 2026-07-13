package com.procurementsaas.template.web;

import com.procurementsaas.template.tenancy.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Sample endpoint demonstrating the wiring every service inherits: JWT principal,
 * feature-level authorization, and the resolved tenant context.
 */
@RestController
@RequestMapping("/api/ping")
public class PingController {

    @GetMapping
    @PreAuthorize("hasAuthority('FEATURE_PING_VIEW')")
    public Map<String, Object> ping(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "service", "service-template",
            "tenant", TenantContext.getTenant(),
            "user", jwt != null ? jwt.getSubject() : "anonymous",
            "timestamp", Instant.now().toString()
        );
    }
}
