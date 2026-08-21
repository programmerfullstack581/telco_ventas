package com.telco.ventas.controller;

import com.telco.ventas.dto.ComisionDto;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.service.ComisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comisiones")
@RequiredArgsConstructor
@Tag(name = "Comisiones", description = "Comisiones de ventas aprobadas")
public class ComisionController {

    private final ComisionService comisionService;

    @GetMapping
    @PreAuthorize("hasAuthority('COMISIONES_VER')")
    @Operation(summary = "Listar comisiones", description = "Comisiones de los agentes del equipo")
    public ResponseEntity<List<ComisionDto>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(comisionService.listar(null));
    }

    @PostMapping("/{id}/pagar")
    @PreAuthorize("hasAuthority('COMISIONES_EDITAR')")
    @Operation(summary = "Marcar comisión como pagada")
    public ResponseEntity<ComisionDto> pagar(@PathVariable Long id,
                                             @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(comisionService.marcarPagada(id, usuario));
    }
}
