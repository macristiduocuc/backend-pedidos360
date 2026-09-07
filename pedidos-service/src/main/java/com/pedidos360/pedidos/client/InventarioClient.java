package com.pedidos360.pedidos.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Encapsula toda la comunicación HTTP con el microservicio de Inventario. */
@Component
public class InventarioClient {

    private final RestClient restClient;

    public InventarioClient(@Value("${inventario.service.url}") String inventarioUrl) {
        this.restClient = RestClient.create(inventarioUrl);
    }

    public ProductoInventarioResponse obtenerProducto(Long productoId) {
        try {
            return restClient.get()
                    .uri("/api/productos/{id}", productoId)
                    .retrieve()
                    .body(ProductoInventarioResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto " + productoId + " no existe en Inventario");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Inventario no respondió correctamente");
        }
    }

    /** Descuenta stock de forma atómica en Inventario. Lanza 409 si ya no alcanza. */
    public void reservarStock(Long productoId, Integer cantidad) {
        try {
            restClient.patch()
                    .uri("/api/productos/{id}/reservar-stock", productoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReservaStockRequest(cantidad))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No queda stock suficiente de \"" + productoId + "\" para completar el pedido");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Inventario no respondió correctamente al reservar stock");
        }
    }
}
