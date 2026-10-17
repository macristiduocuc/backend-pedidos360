package com.pedidos360.rabbitadmin.controller;

import com.pedidos360.rabbitadmin.dto.ColaRequest;
import com.pedidos360.rabbitadmin.dto.ColaResponse;
import com.pedidos360.rabbitadmin.exception.GlobalExceptionHandler;
import com.pedidos360.rabbitadmin.exception.RabbitAdminException;
import com.pedidos360.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba los endpoints REST: rutas, verbos HTTP y códigos de respuesta.
 * (La seguridad por rol se prueba aparte, con el JWT real de Azure.)
 */
class ControladoresTest {

    private final RabbitAdminService servicio = mock(RabbitAdminService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ColaController(servicio), new ExchangeController(servicio), new BindingController(servicio))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void crearUnaColaConNombreVacioDevuelve400() throws Exception {
        String cuerpo = mvc.perform(post("/api/queues").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("nombre"));
    }

    @Test
    void crearUnaColaValidaDevuelve201() throws Exception {
        when(servicio.crearCola(any(ColaRequest.class))).thenReturn(new ColaResponse("demo.q", 0, 0));

        String cuerpo = mvc.perform(post("/api/queues").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"demo.q\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("demo.q"));
    }

    @Test
    void consultarUnaColaInexistenteDevuelve404() throws Exception {
        when(servicio.obtenerCola("no.existe")).thenThrow(RabbitAdminException.noEncontrado("La cola 'no.existe' no existe"));

        mvc.perform(get("/api/queues/no.existe")).andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnaColaDevuelve204() throws Exception {
        mvc.perform(delete("/api/queues/demo.q")).andExpect(status().isNoContent());

        verify(servicio).eliminarCola("demo.q");
    }

    @Test
    void eliminarUnaColaProtegidaDevuelve409() throws Exception {
        doThrow(RabbitAdminException.conflicto("La cola es parte del sistema de pedidos y no se puede eliminar"))
                .when(servicio).eliminarCola("cocina.comanda.q");

        String cuerpo = mvc.perform(delete("/api/queues/cocina.comanda.q"))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("sistema de pedidos"));
    }

    @Test
    void crearUnExchangeConUnTipoInexistenteDevuelve400() throws Exception {
        mvc.perform(post("/api/exchanges").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo.ex\",\"tipo\":\"inventado\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarUnBindingSinParametrosDevuelve400() throws Exception {
        mvc.perform(delete("/api/bindings")).andExpect(status().isBadRequest());
    }
}
