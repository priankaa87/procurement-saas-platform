package com.procurementsaas.identity.web;

import com.procurementsaas.identity.tenancy.TenantContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.security.core.Authentication;

/**
 * Returns the current caller's identity, tenant, and effective authorities, derived from
 * the validated JWT. Available to any authenticated user.
 */
@RestController
@RequestMapping("/me")
public class MeController {

    @GetMapping
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        var authorities = new TreeSet<String>();
        if (authentication != null) {
            for (GrantedAuthority a : authentication.getAuthorities()) {
                authorities.add(a.getAuthority());
            }
        }
        return Map.of(
            "subject", String.valueOf(jwt.getSubject()),
            "username", String.valueOf(jwt.getClaimAsString("preferred_username")),
            "email", String.valueOf(jwt.getClaimAsString("email")),
            "tenant", TenantContext.getTenant(),
            "authorities", List.copyOf(authorities)
        );
    }
}
