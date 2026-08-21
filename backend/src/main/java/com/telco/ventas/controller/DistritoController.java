package com.telco.ventas.controller;

import com.telco.ventas.dto.DistritoDto;
import com.telco.ventas.service.DistritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/distritos")
@RequiredArgsConstructor
@Tag(name = "Distritos", description = "Catálogo de distritos / geografía")
public class DistritoController {

    private final DistritoService distritoService;

    @GetMapping
    @Operation(summary = "Listar distritos")
    public ResponseEntity<List<DistritoDto.Response>> listar() {
        return ResponseEntity.ok(distritoService.listar());
    }
}
