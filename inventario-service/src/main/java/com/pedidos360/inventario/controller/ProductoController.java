package com.pedidos360.inventario.controller;

import com.pedidos360.inventario.dto.ProductoRequest;
import com.pedidos360.inventario.dto.ProductoResponse;
import com.pedidos360.inventario.dto.ReservaStockRequest;
import com.pedidos360.inventario.model.Categoria;
import com.pedidos360.inventario.model.Local;
import com.pedidos360.inventario.model.Producto;
import com.pedidos360.inventario.repository.CategoriaRepository;
import com.pedidos360.inventario.repository.LocalRepository;
import com.pedidos360.inventario.repository.ProductoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final LocalRepository localRepository;

    public ProductoController(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               LocalRepository localRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.localRepository = localRepository;
    }

    @GetMapping
    public List<ProductoResponse> listar(@RequestParam(required = false) String categoria) {
        List<Producto> productos = (categoria == null || categoria.isBlank())
                ? productoRepository.findByActivoTrue()
                : productoRepository.findByActivoTrueAndCategoria_NombreIgnoreCase(categoria);

        return productos.stream().map(ProductoResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return ProductoResponse.desde(buscarOFallar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        Producto producto = new Producto();
        aplicarRequest(producto, request);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        Producto producto = buscarOFallar(id);
        aplicarRequest(producto, request);
        return ProductoResponse.desde(productoRepository.save(producto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        Producto producto = buscarOFallar(id);
        // Baja lógica: no borramos la fila (hay pedidos históricos que la referencian).
        producto.setActivo(false);
        productoRepository.save(producto);
        return ResponseEntity.noContent().build();
    }

    /**
     * Llamado por el microservicio de Pedidos al confirmar una compra.
     * Devuelve 409 si no alcanza el stock; el UPDATE de reservarStock() es atómico.
     */
    @PatchMapping("/{id}/reservar-stock")
    @Transactional
    public ResponseEntity<Void> reservarStock(@PathVariable Long id, @Valid @RequestBody ReservaStockRequest request) {
        buscarOFallar(id); // 404 si el producto no existe
        int filasActualizadas = productoRepository.reservarStock(id, request.cantidad());
        if (filasActualizadas == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin stock suficiente para el producto " + id);
        }
        return ResponseEntity.ok().build();
    }

    private Producto buscarOFallar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto " + id + " no existe"));
    }

    private void aplicarRequest(Producto producto, ProductoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría " + request.categoriaId() + " no existe"));
        Local local = localRepository.findById(request.localId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local " + request.localId() + " no existe"));

        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setImagenUrl(request.imagenUrl());
        producto.setCategoria(categoria);
        producto.setLocal(local);
        if (producto.getActivo() == null) {
            producto.setActivo(true);
        }
    }
}
