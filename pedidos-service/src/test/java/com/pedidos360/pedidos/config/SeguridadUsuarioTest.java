package com.pedidos360.pedidos.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeguridadUsuarioTest {

    private final SeguridadUsuario seguridad = new SeguridadUsuario();

    private JwtAuthenticationToken tokenDe(String preferredUsername) {
        Jwt.Builder builder = Jwt.withTokenValue("token-de-prueba").header("alg", "none").claim("sub", "usuario-1");
        if (preferredUsername != null) {
            builder.claim("preferred_username", preferredUsername);
        }
        return new JwtAuthenticationToken(builder.build());
    }

    @Test
    void permiteSiElCorreoCoincideSinImportarMayusculas() {
        assertTrue(seguridad.esElMismoUsuario("Cliente@Pedidos360.com", tokenDe("cliente@pedidos360.com")));
    }

    @Test
    void rechazaElCorreoDeOtroUsuario() {
        assertFalse(seguridad.esElMismoUsuario("otro@pedidos360.com", tokenDe("cliente@pedidos360.com")));
    }

    @Test
    void rechazaSiElTokenNoTraePreferredUsername() {
        assertFalse(seguridad.esElMismoUsuario("cliente@pedidos360.com", tokenDe(null)));
    }

    @Test
    void rechazaCorreoNuloOAutenticacionNula() {
        assertFalse(seguridad.esElMismoUsuario(null, tokenDe("cliente@pedidos360.com")));
        assertFalse(seguridad.esElMismoUsuario("cliente@pedidos360.com", null));
    }

    @Test
    void rechazaAutenticacionQueNoEsJwt() {
        assertFalse(seguridad.esElMismoUsuario("cliente@pedidos360.com",
                new TestingAuthenticationToken("cliente@pedidos360.com", "x")));
    }
}