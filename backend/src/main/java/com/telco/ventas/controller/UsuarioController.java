package com.telco.ventas.controller;

import com.telco.ventas.dto.UsuarioDto;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.service.UsuarioAdminService;
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
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USUARIOS_VER')")
@Tag(name = "Usuarios", description = "Gestión de usuarios (solo ADMIN)")
public class UsuarioController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping
    @Operation(summary = "Listar usuarios")
    public ResponseEntity<List<UsuarioDto.Response>> listar() {
        return ResponseEntity.ok(usuarioAdminService.listar());
    }

    @GetMapping("/opciones")
    @Operation(summary = "Listar agentes y supervisores", description = "Para asignar supervisores")
    public ResponseEntity<List<UsuarioDto.Response>> opciones() {
        return ResponseEntity.ok(usuarioAdminService.listarAgentesYSupervisores());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario")
    public ResponseEntity<UsuarioDto.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioAdminService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIOS_CREAR')")
    @Operation(summary = "Crear usuario")
    public ResponseEntity<UsuarioDto.Response> crear(@Valid @RequestBody UsuarioDto.Request request,
                                                     @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioAdminService.crear(request, usuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_EDITAR')")
    @Operation(summary = "Actualizar usuario", description = "Cambiar rol, supervisor, estado o contraseña")
    public ResponseEntity<UsuarioDto.Response> actualizar(@PathVariable Long id,
                                                          @RequestBody UsuarioDto.UpdateRequest request,
                                                          @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuarioAdminService.actualizar(id, request, usuario));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('USUARIOS_EDITAR')")
    @Operation(summary = "Inhabilitar / habilitar usuario", description = "activo=true habilita, false inhabilita. Sin parámetro alterna el estado")
    public ResponseEntity<UsuarioDto.Response> cambiarEstado(@PathVariable Long id,
                                                             @RequestParam(required = false) Boolean activo,
                                                             @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuarioAdminService.cambiarEstado(id, activo, usuario));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIOS_ELIMINAR')")
    @Operation(summary = "Eliminar usuario", description = "Borra el usuario y sus dependencias (ventas, comisiones, historial, auditoría) de la base de datos")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @AuthenticationPrincipal Usuario usuario) {
        usuarioAdminService.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
