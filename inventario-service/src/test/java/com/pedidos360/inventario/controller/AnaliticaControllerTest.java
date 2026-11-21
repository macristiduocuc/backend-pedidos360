package com.pedidos360.inventario.controller;

import com.pedidos360.inventario.dto.ProductoVendidoResponse;
import com.pedidos360.inventario.service.AnaliticaVentasService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnaliticaControllerTest {

    private final AnaliticaVentasService servicio = mock(AnaliticaVentasService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AnaliticaController(servicio)).build();

    @Test
    void sinIndicarLimiteDevuelveLos10MasVendidos() throws Exception {
        when(servicio.productosMasVendidos(10)).thenReturn(List.of(new ProductoVendidoResponse(11L, "Empanada", 8)));

        String cuerpo = mvc.perform(get("/api/analitica/productos-mas-vendidos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("Empanada"));
    }

    @Test
    void elLimiteSePasaAlServicio() throws Exception {
        mvc.perform(get("/api/analitica/productos-mas-vendidos").param("limite", "5")).andExpect(status().isOk());

        verify(servicio).productosMasVendidos(5);
    }
}