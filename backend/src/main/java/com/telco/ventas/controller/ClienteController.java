package com.telco.ventas.controller;

import com.telco.ventas.dto.ClienteDto;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Catálogo de clientes")
@PreAuthorize("hasAuthority('CLIENTES_VER')")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @Operation(summary = "Listar clientes", description = "Búsqueda por DNI o nombre con paginación")
    public ResponseEntity<Page<ClienteDto.Response>> listar(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(clienteService.listar(search, pageable));
    }

    @GetMapping("/dni/{dni}")
    @Operation(summary = "Buscar cliente por DNI")
    public ResponseEntity<ClienteDto.Response> buscarPorDni(@PathVariable String dni) {
        return ResponseEntity.ok(clienteService.buscarPorDni(dni));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por id")
    public ResponseEntity<ClienteDto.Response> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTES_CREAR')")
    @Operation(summary = "Crear cliente")
    public ResponseEntity<ClienteDto.Response> crear(@Valid @RequestBody ClienteDto.Request request,
                                                     @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request, usuario));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_EDITAR')")
    @Operation(summary = "Actualizar cliente")
    public ResponseEntity<ClienteDto.Response> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody ClienteDto.Request request,
                                                          @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(clienteService.actualizar(id, request, usuario));
    }
}
