package com.pedidos360.inventario.config;

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
            .cors(cors -> {}) // usa el CorsConfig (WebMvcConfigurer) que ya existe
            .sessionManagement(session -> session.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Llamada interna de pedidos-service: cualquier usuario autenticado alcanza,
                // no es una acción "de rol" sino parte del flujo de compra del cliente.
                .requestMatchers(HttpMethod.PATCH, "/api/productos/*/reservar-stock").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**", "/api/locales/**").authenticated()
                // Gestión de catálogo/inventario: por ahora, rol Auditoría (nuestro "admin" de facto).
                .requestMatchers(HttpMethod.POST, "/api/productos").hasRole("AUDITORIA")
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasRole("AUDITORIA")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("AUDITORIA")
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

    /** Azure AD entrega los roles del usuario en el claim "roles" (array de strings). */
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
