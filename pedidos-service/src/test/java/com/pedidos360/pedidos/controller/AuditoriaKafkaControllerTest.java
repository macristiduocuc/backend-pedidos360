package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.dto.AuditoriaKafkaResponse;
import com.pedidos360.pedidos.service.AuditoriaKafkaService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditoriaKafkaControllerTest {

    private final AuditoriaKafkaService servicio = mock(AuditoriaKafkaService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuditoriaKafkaController(servicio)).build();

    private AuditoriaKafkaResponse respuesta() {
        return new AuditoriaKafkaResponse(1L, "pedidos.eventos", 2, 15L, "7", "CREADO", 7L, 3L,
                "Pendiente", new BigDecimal("1990.00"), null, null);
    }

    @Test
    void sinFiltroDevuelveElHistorialReciente() throws Exception {
        when(servicio.listar(null)).thenReturn(List.of(respuesta()));

        String cuerpo = mvc.perform(get("/api/auditoria-kafka"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("CREADO"));
    }

    @Test
    void conPedidoIdFiltraLosEventosDeEsePedido() throws Exception {
        when(servicio.listar(7L)).thenReturn(List.of(respuesta()));

        mvc.perform(get("/api/auditoria-kafka").param("pedidoId", "7")).andExpect(status().isOk());

        verify(servicio).listar(7L);
    }
}