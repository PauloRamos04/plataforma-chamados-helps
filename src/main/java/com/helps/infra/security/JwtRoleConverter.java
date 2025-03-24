package com.helps.infra.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        System.out.println("JWT autenticado:");
        System.out.println("- Subject: " + jwt.getSubject());
        System.out.println("- Authorities: " + authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", ")));

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (jwt.hasClaim("roles")) {
            try {
                @SuppressWarnings("unchecked")
                List<String> roles = jwt.getClaimAsStringList("roles");
                if (roles != null) {
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao extrair roles do JWT: " + e.getMessage());
            }
        }

        if (jwt.hasClaim("scope")) {
            String scopes = jwt.getClaimAsString("scope");
            if (scopes != null) {
                for (String scope : scopes.split(" ")) {
                    authorities.add(new SimpleGrantedAuthority(scope));
                }
            }
        }

        if (jwt.hasClaim("authorities")) {
            try {
                @SuppressWarnings("unchecked")
                List<String> authsList = jwt.getClaimAsStringList("authorities");
                if (authsList != null) {
                    for (String auth : authsList) {
                        authorities.add(new SimpleGrantedAuthority(auth));
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao extrair authorities do JWT: " + e.getMessage());
            }
        }

        if (jwt.hasClaim("realm_access")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                if (realmAccess != null && realmAccess.containsKey("roles")) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) realmAccess.get("roles");
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao extrair realm_access do JWT: " + e.getMessage());
            }
        }

        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }
}