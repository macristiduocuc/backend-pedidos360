package com.pedidos360.kafkaadmin.controller;

import com.pedidos360.kafkaadmin.dto.TopicRequest;
import com.pedidos360.kafkaadmin.dto.TopicResponse;
import com.pedidos360.kafkaadmin.exception.GlobalExceptionHandler;
import com.pedidos360.kafkaadmin.exception.KafkaAdminException;
import com.pedidos360.kafkaadmin.service.KafkaAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba los endpoints REST: rutas, verbos HTTP y códigos de respuesta.
 * (La seguridad por rol se prueba aparte, con el JWT real de Azure.)
 */
class ControladoresTest {

    private final KafkaAdminService servicio = mock(KafkaAdminService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new TopicController(servicio), new ConsumerGroupController(servicio))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void crearUnTopicoConNombreVacioDevuelve400() throws Exception {
        String cuerpo = mvc.perform(post("/api/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"particiones\":3,\"replicacion\":2}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("nombre"));
    }

    @Test
    void crearUnTopicoValidoDevuelve201() throws Exception {
        when(servicio.crearTopico(any(TopicRequest.class)))
                .thenReturn(new TopicResponse("demo.admin", 3, 2, Map.of(), List.of()));

        String cuerpo = mvc.perform(post("/api/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo.admin\",\"particiones\":3,\"replicacion\":2}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("demo.admin"));
    }

    @Test
    void consultarUnTopicoInexistenteDevuelve404() throws Exception {
        when(servicio.obtenerTopico("no.existe")).thenThrow(KafkaAdminException.noEncontrado("El tópico no existe"));

        mvc.perform(get("/api/topics/no.existe")).andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnTopicoDevuelve204() throws Exception {
        mvc.perform(delete("/api/topics/demo.admin")).andExpect(status().isNoContent());

        verify(servicio).eliminarTopico("demo.admin");
    }

    @Test
    void eliminarUnTopicoDelSistemaDevuelve409() throws Exception {
        doThrow(KafkaAdminException.conflicto("El tópico es parte del sistema de pedidos y no se puede eliminar"))
                .when(servicio).eliminarTopico("pedidos.eventos");

        String cuerpo = mvc.perform(delete("/api/topics/pedidos.eventos"))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("sistema de pedidos"));
    }

    @Test
    void cambiarLaConfiguracionSinValoresDevuelve400() throws Exception {
        mvc.perform(patch("/api/topics/demo.admin/config").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configuracion\":{}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarLosConsumerGroupsDevuelve200() throws Exception {
        when(servicio.listarGrupos()).thenReturn(List.of("auditoria", "analitica-ventas"));

        String cuerpo = mvc.perform(get("/api/consumer-groups"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("auditoria"));
    }
}
