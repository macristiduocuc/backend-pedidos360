package com.pedidos360.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${pedidos360.security.audience}")
    private String audiencia;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .sessionManagement(session -> session.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Cliente: crear pedido y ver su propio historial.
                .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/pedidos/cliente/**").hasRole("CLIENTE")
                // Cocina: cambiar estado Pendiente -> En preparación -> Hecho.
                .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/estado").hasRole("COCINA")
                // Despacho: marcar entregado.
                .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/entregar").hasRole("DESPACHO")
                // Auditoría: dashboards de supervisión.
                .requestMatchers(HttpMethod.GET, "/api/pedidos/resumen").hasRole("AUDITORIA")
                .requestMatchers(HttpMethod.GET, "/api/eventos-auditoria").hasRole("AUDITORIA")
                // Listados generales de pedidos: cocina, despacho y auditoría los necesitan cada uno para su panel.
                .requestMatchers(HttpMethod.GET, "/api/pedidos/**").hasAnyRole("COCINA", "DESPACHO", "AUDITORIA")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeRoles())));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);
        OAuth2TokenValidator<Jwt> conIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> conAudiencia = new AudienceValidator(audiencia);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(conIssuer, conAudiencia));
        return decoder;
    }

    private JwtAuthenticationConverter conversorDeRoles() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) return List.of();
            return roles.stream()
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                    .toList();
        });
        return converter;
    }
}
