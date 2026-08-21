package com.telco.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class RolDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Nombre del rol es obligatorio")
        @Size(max = 50, message = "Nombre máximo 50 caracteres")
        private String nombre;

        @Size(max = 255, message = "Descripción máxima 255 caracteres")
        private String descripcion;

        private Boolean activo;

        private List<String> permisos;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String nombre;
        private String descripcion;
        private Boolean activo;
        private Set<String> permisos;
        private Long usuarios;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermisoResponse {
        private Long id;
        private String codigo;
        private String modulo;
        private String accion;
        private String descripcion;
    }
}
