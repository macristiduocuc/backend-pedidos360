package com.pedidos360.pedidos.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Comprueba que el destinatario se deduce del token y nunca de un dato enviado por el navegador. */
class DestinatarioTest {

    private JwtAuthenticationToken token(String rol, String preferredUsername) {
        Jwt.Builder builder = Jwt.withTokenValue("token-de-prueba").header("alg", "none").claim("sub", "usuario-1");
        if (preferredUsername != null) {
            builder.claim("preferred_username", preferredUsername);
        }
        return new JwtAuthenticationToken(builder.build(), List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    @Test
    void auditoriaRecibeLasNotificacionesDeSuRol() {
        Destinatario destinatario = Destinatario.de(token("AUDITORIA", "auditor@pedidos360.com"));

        assertTrue(destinatario.esDeRol());
        assertEquals("AUDITORIA", destinatario.rol());
        assertNull(destinatario.correo());
    }

    @Test
    void elClienteRecibeLasNotificacionesDeSuCorreo() {
        Destinatario destinatario = Destinatario.de(token("CLIENTE", "ana@correo.cl"));

        assertFalse(destinatario.esDeRol());
        assertEquals("ana@correo.cl", destinatario.correo());
        assertNull(destinatario.rol());
    }

    @Test
    void unClienteSinCorreoEnElTokenSeRechaza() {
        assertThrows(AccessDeniedException.class, () -> Destinatario.de(token("CLIENTE", null)));
    }

    @Test
    void unaAutenticacionQueNoEsJwtDeClienteSeRechaza() {
        assertThrows(AccessDeniedException.class,
                () -> Destinatario.de(new TestingAuthenticationToken("ana", "x", "ROLE_CLIENTE")));
    }

    @Test
    void sinAutenticacionSeRechaza() {
        assertThrows(AccessDeniedException.class, () -> Destinatario.de(null));
    }
}