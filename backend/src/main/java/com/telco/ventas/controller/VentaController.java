package com.telco.ventas.controller;

import com.telco.ventas.dto.CreateVentaRequest;
import com.telco.ventas.dto.RechazarVentaRequest;
import com.telco.ventas.dto.ResumenVentasResponse;
import com.telco.ventas.dto.VentaHistorialDto;
import com.telco.ventas.dto.VentaResponse;
import com.telco.ventas.entity.EstadoVenta;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.service.ReporteService;
import com.telco.ventas.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "API de gestión de ventas Telco")
public class VentaController {

    private final VentaService ventaService;
    private final ReporteService reporteService;

    @PostMapping
    @PreAuthorize("hasAuthority('VENTAS_CREAR')")
    @Operation(summary = "Crear venta (Agente)", description = "Crea una venta en estado PENDIENTE")
    public ResponseEntity<VentaResponse> crearVenta(
            @Valid @RequestBody CreateVentaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        VentaResponse response = ventaService.crearVenta(request, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mis-ventas")
    @PreAuthorize("hasAuthority('VENTAS_VER')")
    @Operation(summary = "Mis ventas (Agente)", description = "Lista las ventas del agente autenticado con filtros y paginación")
    public ResponseEntity<Page<VentaResponse>> misVentas(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Usuario usuario) {

        LocalDateTime desdeDt = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDt = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

        return ResponseEntity.ok(ventaService.misVentas(usuario.getId(), estado, desdeDt, hastaDt, pageable));
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAuthority('VENTAS_VER')")
    @Operation(summary = "Ventas pendientes (Backoffice)", description = "Lista ventas en estado PENDIENTE")
    public ResponseEntity<Page<VentaResponse>> ventasPendientes(
            @PageableDefault(size = 10, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ventaService.ventasPendientes(pageable));
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAuthority('VENTAS_APROBAR')")
    @Operation(summary = "Aprobar venta", description = "Cambia estado a APROBADA, setea fecha de validación y genera comisión")
    public ResponseEntity<VentaResponse> aprobarVenta(@PathVariable Long id,
                                                      @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ventaService.aprobarVenta(id, usuario));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAuthority('VENTAS_RECHAZAR')")
    @Operation(summary = "Rechazar venta", description = "Cambia estado a RECHAZADA requiriendo motivo")
    public ResponseEntity<VentaResponse> rechazarVenta(
            @PathVariable Long id,
            @Valid @RequestBody RechazarVentaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(ventaService.rechazarVenta(id, request, usuario));
    }

    @GetMapping("/equipo")
    @PreAuthorize("hasAuthority('VENTAS_VER')")
    @Operation(summary = "Ventas del equipo", description = "Ventas de agentes bajo el supervisor (Backoffice/Admin ven todas)")
    public ResponseEntity<List<VentaResponse>> ventasEquipo(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) Long agenteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal Usuario usuario) {

        LocalDateTime desdeDt = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDt = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

        return ResponseEntity.ok(ventaService.ventasEquipo(usuario, estado, agenteId, desdeDt, hastaDt));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAuthority('VENTAS_VER')")
    @Operation(summary = "Historial de estados de una venta", description = "Trazabilidad de cambios de estado")
    public ResponseEntity<List<VentaHistorialDto>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.historialVenta(id));
    }

    @GetMapping("/reportes/resumen")
    @PreAuthorize("hasAuthority('REPORTES_VER')")
    @Operation(summary = "Resumen de ventas (alias)", description = "Alias de /api/v1/reportes/resumen por compatibilidad")
    public ResponseEntity<ResumenVentasResponse> resumenVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(reporteService.resumenVentas(usuario, dia, anio, mes, desde, hasta));
    }
}
