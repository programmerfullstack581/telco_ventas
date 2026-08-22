package com.telco.ventas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health", description = "Endpoints de estado y verificación de servicio")
public class HealthController {

    @GetMapping("/")
    @Operation(summary = "Root status", description = "Verifica que el servicio esté activo")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "Ventas Telco Fija Hogar",
                "version", "1.0.0",
                "swagger", "/swagger-ui.html"
        ));
    }

    @GetMapping("/api/health")
    @Operation(summary = "Health check", description = "Endpoint de salud del backend")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
