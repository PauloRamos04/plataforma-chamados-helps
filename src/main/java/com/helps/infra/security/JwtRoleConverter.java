package com.helps.infra.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<String> authorities = jwt.getClaimAsStringList("roles");

        System.out.println("Roles extraídas do token: " + authorities);

        // Converter diretamente para SimpleGrantedAuthority sem adicionar prefixos
        Set<SimpleGrantedAuthority> grantedAuthorities =
                authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, grantedAuthorities);
    }
}