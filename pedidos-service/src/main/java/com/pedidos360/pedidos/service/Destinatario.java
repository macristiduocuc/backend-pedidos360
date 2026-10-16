package com.pedidos360.pedidos.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * A quién pertenecen las notificaciones que se están consultando:
 *  - Auditoría: las notificaciones dirigidas a su rol (por ejemplo, alertas de stock bajo).
 *  - Cliente: las dirigidas a su correo (el del token de Azure).
 * Se calcula SIEMPRE a partir del token, nunca de un dato que envíe el navegador,
 * así nadie puede leer las notificaciones de otra persona.
 */
public record Destinatario(String correo, String rol) {

    public static final String ROL_AUDITORIA = "AUDITORIA";

    public static Destinatario delCliente(String correo) {
        return new Destinatario(correo, null);
    }

    public static Destinatario delRol(String rol) {
        return new Destinatario(null, rol);
    }

    public boolean esDeRol() {
        return rol != null;
    }

    public static Destinatario de(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("No hay una sesión autenticada");
        }
        boolean esAuditoria = authentication.getAuthorities().stream()
                .anyMatch(autoridad -> ("ROLE_" + ROL_AUDITORIA).equals(autoridad.getAuthority()));
        if (esAuditoria) {
            return delRol(ROL_AUDITORIA);
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String correo = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (correo != null && !correo.isBlank()) {
                return delCliente(correo);
            }
        }
        throw new AccessDeniedException("No se pudo identificar al destinatario de las notificaciones");
    }
}