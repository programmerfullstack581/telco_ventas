package com.telco.ventas.controller;

import com.telco.ventas.dto.ReporteDto;
import com.telco.ventas.dto.ResumenVentasResponse;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.service.ReporteExportService;
import com.telco.ventas.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes para Supervisor/Admin")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteExportService reporteExportService;

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('REPORTES_VER')")
    @Operation(summary = "Resumen de ventas", description = "Conteos por estado, monto total aprobadas y serie diaria")
    public ResponseEntity<ResumenVentasResponse> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reporteService.resumenVentas(usuario, dia, anio, mes, desde, hasta));
    }

    @GetMapping("/por-plan")
    @PreAuthorize("hasAuthority('REPORTES_VER')")
    @Operation(summary = "Ventas por plan", description = "Agrupación de ventas por plan en el período")
    public ResponseEntity<List<ReporteDto.PorPlan>> porPlan(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reporteService.ventasPorPlan(usuario, dia, anio, mes, desde, hasta));
    }

    @GetMapping("/por-agente")
    @PreAuthorize("hasAuthority('REPORTES_VER')")
    @Operation(summary = "Ventas por agente", description = "Agrupación de ventas por agente en el período")
    public ResponseEntity<List<ReporteDto.PorAgente>> porAgente(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reporteService.ventasPorAgente(usuario, dia, anio, mes, desde, hasta));
    }

    @GetMapping("/comisiones")
    @PreAuthorize("hasAuthority('REPORTES_VER')")
    @Operation(summary = "Resumen de comisiones", description = "Comisiones por agente (pendientes y pagadas)")
    public ResponseEntity<ReporteDto.ResumenComisiones> comisiones(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reporteService.resumenComisiones(usuario));
    }

    @GetMapping("/exportar")
    @PreAuthorize("hasAuthority('REPORTES_EXPORTAR')")
    @Operation(summary = "Exportar reporte", description = "Genera el reporte en Excel, PDF, CSV o HTML con diseño propio según el formato")
    public ResponseEntity<byte[]> exportar(
            @RequestParam String tipo,
            @RequestParam(defaultValue = "xlsx") String formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @AuthenticationPrincipal Usuario usuario) {

        String tipoNorm = tipo.toLowerCase();
        String formatoNorm = formato.toLowerCase();
        if (!List.of("resumen", "por-plan", "por-agente", "comisiones").contains(tipoNorm)) {
            return ResponseEntity.badRequest().build();
        }
        if (!List.of("xlsx", "excel", "pdf", "csv", "html").contains(formatoNorm)) {
            return ResponseEntity.badRequest().build();
        }

        byte[] data = reporteExportService.exportar(usuario, tipoNorm, formatoNorm, dia, anio, mes, desde, hasta);
        String filename = reporteExportService.nombreArchivo(tipoNorm, formatoNorm);

        ContentDisposition cd = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentType(MediaType.parseMediaType(reporteExportService.contentType(formatoNorm)))
                .body(data);
    }
}
