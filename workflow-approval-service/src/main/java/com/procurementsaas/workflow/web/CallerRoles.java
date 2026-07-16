package com.procurementsaas.workflow.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the caller's identity and roles from their token.
 *
 * <p>Both come from the authenticated principal, never from the request body — an approval
 * where the client says who it is would be no approval at all.
 */
final class CallerRoles {

    private static final String ROLE_PREFIX = "ROLE_";

    private CallerRoles() {
    }

    /** The role codes the caller holds, with Spring's {@code ROLE_} prefix stripped. */
    static Set<String> of(Authentication authentication) {
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith(ROLE_PREFIX))
            .map(a -> a.substring(ROLE_PREFIX.length()))
            .collect(Collectors.toSet());
    }

    static String actorId(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
