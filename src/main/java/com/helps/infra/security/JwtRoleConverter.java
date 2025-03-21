package com.helps.infra.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);

        // Extrair roles do token
        Map<String, Object> claims = jwt.getClaims();
        Collection<String> roles = Collections.emptyList();

        if (claims.containsKey("roles")) {
            roles = (Collection<String>) claims.get("roles");
        }

        // Converter roles para authorities
        Collection<GrantedAuthority> roleAuthorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Combinar authorities padrão com as baseadas em roles
        Collection<GrantedAuthority> allAuthorities = Stream.concat(
                defaultAuthorities.stream(),
                roleAuthorities.stream()
        ).collect(Collectors.toList());

        return new JwtAuthenticationToken(jwt, allAuthorities, jwt.getSubject());
    }
}