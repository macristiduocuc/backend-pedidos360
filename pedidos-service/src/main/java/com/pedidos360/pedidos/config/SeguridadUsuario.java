package com.pedidos360.pedidos.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("seguridad")
public class SeguridadUsuario {

    public boolean esElMismoUsuario(String correo, Authentication authentication) {
        if (correo == null || authentication == null) {
            return false;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String usuario = jwtAuth.getToken().getClaimAsString("preferred_username");
            return usuario != null && usuario.equalsIgnoreCase(correo.trim());
        }
        return false;
    }
}