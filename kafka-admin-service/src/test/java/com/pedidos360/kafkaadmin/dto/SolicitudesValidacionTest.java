package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Comprueba que las anotaciones de validación de los DTO rechazan los datos inválidos. */
class SolicitudesValidacionTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private <T> List<String> camposInvalidos(T solicitud) {
        Set<ConstraintViolation<T>> violaciones = validador.validate(solicitud);
        return violaciones.stream().map(v -> v.getPropertyPath().toString()).toList();
    }

    @Test
    void unTopicoValidoNoTieneErrores() {
        assertTrue(camposInvalidos(new TopicRequest("demo.admin", 3, 2, Map.of("cleanup.policy", "delete"))).isEmpty());
    }

    @Test
    void elNombreVacioOConEspaciosSeRechaza() {
        assertTrue(camposInvalidos(new TopicRequest("", 3, 2, null)).contains("nombre"));
        assertTrue(camposInvalidos(new TopicRequest("mi topico", 3, 2, null)).contains("nombre"));
    }

    @Test
    void lasParticionesYLaReplicacionDebenEstarEnRango() {
        assertTrue(camposInvalidos(new TopicRequest("demo", 0, 2, null)).contains("particiones"));
        assertTrue(camposInvalidos(new TopicRequest("demo", 3, 9, null)).contains("replicacion"));
        assertTrue(camposInvalidos(new TopicRequest("demo", null, 2, null)).contains("particiones"));
    }

    @Test
    void cambiarConfiguracionSinNingunValorSeRechaza() {
        assertTrue(camposInvalidos(new TopicConfigRequest(Map.of())).contains("configuracion"));
    }

    @Test
    void elTotalDeParticionesEsObligatorio() {
        assertTrue(camposInvalidos(new ParticionesRequest(null)).contains("total"));
    }
}
