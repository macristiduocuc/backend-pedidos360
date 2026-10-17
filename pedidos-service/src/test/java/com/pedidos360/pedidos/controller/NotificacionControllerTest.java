package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.dto.NotificacionResponse;
import com.pedidos360.pedidos.dto.NotificacionesResponse;
import com.pedidos360.pedidos.service.Destinatario;
import com.pedidos360.pedidos.service.NotificacionConsultaService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba rutas, verbos y códigos de la campanita.
 * (La restricción por rol se prueba aparte, con el JWT real de Azure.)
 */
class NotificacionControllerTest {

    private final NotificacionConsultaService servicio = mock(NotificacionConsultaService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new NotificacionController(servicio)).build();

    private final JwtAuthenticationToken cliente = new JwtAuthenticationToken(
            Jwt.withTokenValue("token-de-prueba").header("alg", "none").claim("sub", "u1")
                    .claim("preferred_username", "ana@correo.cl").build(),
            List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

    @Test
    void misNotificacionesDevuelveLaListaYElContadorDeNoLeidas() throws Exception {
        when(servicio.listar(any(Destinatario.class))).thenReturn(new NotificacionesResponse(1L, List.of(
                new NotificacionResponse(1L, 5L, "Recibimos tu pedido #5.", false,
                        OffsetDateTime.parse("2026-10-14T12:00:00Z")))));

        String cuerpo = mvc.perform(get("/api/notificaciones/mias").principal(cliente))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("noLeidas"));
        assertTrue(cuerpo.contains("Recibimos tu pedido"));
        verify(servicio).listar(Destinatario.delCliente("ana@correo.cl"));
    }

    @Test
    void marcarUnaNotificacionPropiaComoLeidaDevuelve204() throws Exception {
        when(servicio.marcarLeida(eq(7L), any(Destinatario.class))).thenReturn(true);

        mvc.perform(patch("/api/notificaciones/7/leida").principal(cliente)).andExpect(status().isNoContent());
    }

    @Test
    void marcarUnaNotificacionAjenaODesconocidaDevuelve404() throws Exception {
        when(servicio.marcarLeida(eq(7L), any(Destinatario.class))).thenReturn(false);

        mvc.perform(patch("/api/notificaciones/7/leida").principal(cliente)).andExpect(status().isNotFound());
    }

    @Test
    void marcarTodasComoLeidasDevuelve204() throws Exception {
        mvc.perform(patch("/api/notificaciones/leidas").principal(cliente)).andExpect(status().isNoContent());

        verify(servicio).marcarTodasLeidas(Destinatario.delCliente("ana@correo.cl"));
    }
}