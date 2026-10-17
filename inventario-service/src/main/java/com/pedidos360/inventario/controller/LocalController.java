package com.pedidos360.inventario.controller;

import com.pedidos360.inventario.model.Local;
import com.pedidos360.inventario.repository.LocalRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locales")
public class LocalController {

    private final LocalRepository localRepository;

    public LocalController(LocalRepository localRepository) {
        this.localRepository = localRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Local> listar() {
        return localRepository.findAll();
    }
}