package com.telco.ventas.controller;

import com.telco.ventas.dto.PlanDto;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planes")
@RequiredArgsConstructor
@Tag(name = "Planes", description = "Catálogo de planes de internet / telefonía fija")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @Operation(summary = "Listar planes", description = "Planes activos para usar en las ventas")
    public ResponseEntity<List<PlanDto.Response>> listarActivos() {
        return ResponseEntity.ok(planService.listarActivos());
    }

    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('PLANES_VER')")
    @Operation(summary = "Listar todos los planes (Admin)")
    public ResponseEntity<List<PlanDto.Response>> listarTodos() {
        return ResponseEntity.ok(planService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener plan por id")
    public ResponseEntity<PlanDto.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(planService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLANES_CREAR')")
    @Operation(summary = "Crear plan (Admin)")
    public ResponseEntity<PlanDto.Response> crear(@Valid @RequestBody PlanDto.Request request,
                                                  @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.crear(request, usuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PLANES_EDITAR')")
    @Operation(summary = "Actualizar plan (Admin)")
    public ResponseEntity<PlanDto.Response> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody PlanDto.Request request,
                                                       @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(planService.actualizar(id, request, usuario));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('PLANES_EDITAR')")
    @Operation(summary = "Activar/desactivar plan (Admin)")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id,
                                              @RequestParam Boolean activo,
                                              @AuthenticationPrincipal Usuario usuario) {
        planService.cambiarEstado(id, activo, usuario);
        return ResponseEntity.noContent().build();
    }
}
