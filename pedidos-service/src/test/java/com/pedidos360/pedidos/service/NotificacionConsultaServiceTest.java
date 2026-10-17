package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.dto.NotificacionesResponse;
import com.pedidos360.pedidos.model.Notificacion;
import com.pedidos360.pedidos.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba lo que la campanita puede ver y modificar: solo lo suyo. */
class NotificacionConsultaServiceTest {

    private final NotificacionRepository repositorio = mock(NotificacionRepository.class);
    private final NotificacionConsultaService servicio = new NotificacionConsultaService(repositorio);

    private final Destinatario ana = Destinatario.delCliente("ana@correo.cl");
    private final Destinatario auditoria = Destinatario.delRol("AUDITORIA");

    @Test
    void elClienteVeSusNotificacionesYCuantasNoHaLeido() {
        Notificacion n = Notificacion.paraCliente(5L, "ana@correo.cl", "Recibimos tu pedido #5.");
        when(repositorio.findByDestinatarioCorreoIgnoreCaseOrderByIdDesc(eq("ana@correo.cl"), any(Pageable.class)))
                .thenReturn(List.of(n));
        when(repositorio.countByDestinatarioCorreoIgnoreCaseAndLeidaFalse("ana@correo.cl")).thenReturn(2L);

        NotificacionesResponse respuesta = servicio.listar(ana);

        assertEquals(2L, respuesta.noLeidas());
        assertEquals(1, respuesta.notificaciones().size());
        assertEquals("Recibimos tu pedido #5.", respuesta.notificaciones().get(0).mensaje());
        assertFalse(respuesta.notificaciones().get(0).leida());
    }

    @Test
    void auditoriaVeLasNotificacionesDeSuRol() {
        Notificacion n = Notificacion.paraRol(null, "AUDITORIA", "Poco stock de Empanada (local 3): quedan 9.");
        when(repositorio.findByDestinatarioRolOrderByIdDesc(eq("AUDITORIA"), any(Pageable.class))).thenReturn(List.of(n));
        when(repositorio.countByDestinatarioRolAndLeidaFalse("AUDITORIA")).thenReturn(1L);

        NotificacionesResponse respuesta = servicio.listar(auditoria);

        assertEquals(1L, respuesta.noLeidas());
        assertEquals(1, respuesta.notificaciones().size());
    }

    @Test
    void marcarComoLeidaUnaNotificacionPropiaLaActualiza() {
        Notificacion n = Notificacion.paraCliente(5L, "ana@correo.cl", "Recibimos tu pedido #5.");
        when(repositorio.findByIdAndDestinatarioCorreoIgnoreCase(7L, "ana@correo.cl")).thenReturn(Optional.of(n));

        boolean marcada = servicio.marcarLeida(7L, ana);

        assertTrue(marcada);
        assertTrue(n.isLeida());
        verify(repositorio).save(n);
    }

    @Test
    void unaNotificacionAjenaSeTrataComoSiNoExistiera() {
        when(repositorio.findByIdAndDestinatarioCorreoIgnoreCase(7L, "ana@correo.cl")).thenReturn(Optional.empty());

        boolean marcada = servicio.marcarLeida(7L, ana);

        assertFalse(marcada);
        verify(repositorio, never()).save(any(Notificacion.class));
    }

    @Test
    void marcarTodasComoLeidasActualizaSoloLasPendientesDelDestinatario() {
        Notificacion a = Notificacion.paraCliente(5L, "ana@correo.cl", "uno");
        Notificacion b = Notificacion.paraCliente(6L, "ana@correo.cl", "dos");
        when(repositorio.findByDestinatarioCorreoIgnoreCaseAndLeidaFalse("ana@correo.cl")).thenReturn(List.of(a, b));

        servicio.marcarTodasLeidas(ana);

        assertTrue(a.isLeida());
        assertTrue(b.isLeida());
        verify(repositorio).saveAll(List.of(a, b));
    }
}