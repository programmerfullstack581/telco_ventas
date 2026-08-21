package com.telco.ventas.controller;

import com.telco.ventas.dto.RolDto;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.service.RolService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Roles y permisos", description = "Gestión de roles dinámicos y permisos (solo ADMIN)")
public class RolController {

    private final RolService rolService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLES_VER')")
    @Operation(summary = "Listar roles")
    public ResponseEntity<List<RolDto.Response>> listar() {
        return ResponseEntity.ok(rolService.listar());
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasAuthority('ROLES_VER')")
    @Operation(summary = "Listar catálogo de permisos")
    public ResponseEntity<List<RolDto.PermisoResponse>> listarPermisos() {
        return ResponseEntity.ok(rolService.listarPermisos());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLES_CREAR')")
    @Operation(summary = "Crear rol")
    public ResponseEntity<RolDto.Response> crear(@Valid @RequestBody RolDto.Request request,
                                                 @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(request, usuario));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES_EDITAR')")
    @Operation(summary = "Actualizar rol")
    public ResponseEntity<RolDto.Response> actualizar(@PathVariable Long id,
                                                      @RequestBody RolDto.Request request,
                                                      @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(rolService.actualizar(id, request, usuario));
    }

    @PutMapping("/roles/{id}/permisos")
    @PreAuthorize("hasAuthority('ROLES_EDITAR')")
    @Operation(summary = "Asignar permisos a un rol")
    public ResponseEntity<RolDto.Response> asignarPermisos(@PathVariable Long id,
                                                           @RequestBody List<String> codigos,
                                                           @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(rolService.asignarPermisos(id, codigos, usuario));
    }

    @PatchMapping("/roles/{id}/estado")
    @PreAuthorize("hasAuthority('ROLES_EDITAR')")
    @Operation(summary = "Habilitar / deshabilitar rol")
    public ResponseEntity<RolDto.Response> cambiarEstado(@PathVariable Long id,
                                                         @RequestParam(required = false) Boolean activo,
                                                         @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(rolService.cambiarEstado(id, activo, usuario));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES_ELIMINAR')")
    @Operation(summary = "Eliminar rol")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @AuthenticationPrincipal Usuario usuario) {
        rolService.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
