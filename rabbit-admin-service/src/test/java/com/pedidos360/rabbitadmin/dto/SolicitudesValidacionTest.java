package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    void unaColaValidaNoTieneErrores() {
        assertTrue(camposInvalidos(new ColaRequest("demo.q", true, true, null, null)).isEmpty());
    }

    @Test
    void elNombreVacioDeUnaColaSeRechaza() {
        assertTrue(camposInvalidos(new ColaRequest("", null, null, null, null)).contains("nombre"));
    }

    @Test
    void elNombreConEspaciosSeRechaza() {
        assertTrue(camposInvalidos(new ColaRequest("mi cola", null, null, null, null)).contains("nombre"));
    }

    @Test
    void unExchangeSinTipoSeRechaza() {
        assertTrue(camposInvalidos(new ExchangeRequest("demo.ex", null, null)).contains("tipo"));
    }

    @Test
    void unBindingSinColaSeRechaza() {
        assertTrue(camposInvalidos(new BindingRequest("pedidos.exchange", "", "demo.#")).contains("cola"));
    }
}
