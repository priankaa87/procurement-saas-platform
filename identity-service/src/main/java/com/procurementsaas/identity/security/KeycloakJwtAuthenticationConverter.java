package com.procurementsaas.identity.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps a Keycloak-issued JWT into Spring Security authorities.
 *
 * <ul>
 *   <li>{@code realm_access.roles} → {@code ROLE_<role>} authorities</li>
 *   <li>a custom {@code features} claim (array of feature codes) → authorities as-is,
 *       so {@code @PreAuthorize("hasAuthority('FEATURE_USER_MANAGE')")} works directly</li>
 * </ul>
 */
public class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            for (Object role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }

        List<String> features = jwt.getClaimAsStringList("features");
        if (features != null) {
            for (String feature : features) {
                authorities.add(new SimpleGrantedAuthority(feature));
            }
        }

        String principalName = jwt.getClaimAsString("preferred_username");
        if (principalName == null) {
            principalName = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }
}
