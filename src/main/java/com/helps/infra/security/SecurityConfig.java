package com.helps.infra.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.public.key}")
    private RSAPublicKey publicKey;

    @Value("${jwt.private.key}")
    private RSAPrivateKey privateKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers("/h2-console/**").permitAll() // Only for development
                        .requestMatchers("/ws/**").permitAll() // WebSockets need separate authentication

                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/admin/users").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/admin/users/{id}").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/users").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/admin/users/{id}").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/admin/users/{id}/status").hasAuthority("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/users").permitAll() // Allow public registration
                        .requestMatchers(HttpMethod.POST, "/register/helper").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/register/admin").hasAuthority("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/chamados/{id}/aderir").hasAnyAuthority("HELPER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/chamados/{id}/aderir").hasAnyAuthority("HELPER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/chamados/{id}/finalizar").hasAnyAuthority("HELPER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/chamados/{id}/finalizar").hasAnyAuthority("HELPER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/chamados/{id}/fechar").hasAnyAuthority("HELPER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/chamados/{id}/fechar").hasAnyAuthority("HELPER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/chamados/{id}/mensagens").authenticated()
                        .requestMatchers(HttpMethod.GET, "/chamados/{id}/mensagens").authenticated()

                        .anyRequest().authenticated())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions().sameOrigin())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/ws/**");
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new JwtRoleConverter();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(List.of("x-auth-token"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        CorsConfiguration wsConfiguration = new CorsConfiguration(configuration);
        wsConfiguration.setAllowedOrigins(List.of("http://localhost:3000"));
        source.registerCorsConfiguration("/ws/**", wsConfiguration);

        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(this.publicKey).privateKey(privateKey).build();
        var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}