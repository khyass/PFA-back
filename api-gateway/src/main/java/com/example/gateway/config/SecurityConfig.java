package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Stream;

/**
 * Reactive Spring Security configuration for the API Gateway.
 * Validates JWTs and enforces role-based access on gateway routes.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers(
                                "/auth/login", "/auth/register/**", "/auth/refresh",
                                "/auth/introspect", "/actuator/health", "/actuator/info"
                        ).permitAll()
                        // Swagger UI
                        .pathMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**").permitAll()
                        // Role-restricted paths
                        .pathMatchers("/enterprise/**").hasRole("ENTERPRISE")
                        .pathMatchers("/candidate/**").hasRole("CANDIDATE")
                        .pathMatchers("/jobs/apply/**").hasRole("CANDIDATE")
                        .pathMatchers("/jobs/create").hasRole("ENTERPRISE")
                        // Everything else requires authentication
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }

    /**
     * Extracts Keycloak realm roles from both realm_access.roles and top-level "roles"
     * claim, mapping each to a ROLE_-prefixed GrantedAuthority.
     */
    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        @SuppressWarnings("unchecked")
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            List<String> realmRoles = Optional.ofNullable(jwt.<Map<String, Object>>getClaim("realm_access"))
                    .map(ra -> (List<String>) ra.get("roles"))
                    .orElse(List.of());

            List<String> topRoles = Optional.ofNullable(jwt.<List<String>>getClaim("roles"))
                    .orElse(List.of());

            return Stream.concat(realmRoles.stream(), topRoles.stream())
                    .distinct()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        }
    }
}
