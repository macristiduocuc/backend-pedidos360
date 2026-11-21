package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.dto.StockActualResponse;
import com.pedidos360.pedidos.service.StockMonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockActualControllerTest {

    private final StockMonitorService servicio = mock(StockMonitorService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new StockActualController(servicio)).build();

    @Test
    void devuelveElUltimoStockDeCadaProducto() throws Exception {
        when(servicio.listar()).thenReturn(List.of(
                new StockActualResponse(11L, "Empanada de pino", 3L, 4, null, null)));

        String cuerpo = mvc.perform(get("/api/stock-actual"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("Empanada de pino"));
    }
}