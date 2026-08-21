package com.telco.ventas.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PlanDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Código de plan es obligatorio")
        @Size(max = 30, message = "Código máximo 30 caracteres")
        private String codigo;

        @NotBlank(message = "Nombre de plan es obligatorio")
        @Size(max = 100, message = "Nombre máximo 100 caracteres")
        private String nombre;

        @NotBlank(message = "Tipo de plan es obligatorio")
        @Size(max = 30, message = "Tipo máximo 30 caracteres")
        private String tipo;

        @Min(value = 1, message = "Velocidad debe ser mayor a 0")
        private Integer velocidadMbps;

        @NotNull(message = "Precio base es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "Precio base debe ser mayor a 0")
        private BigDecimal precioBase;

        @Size(max = 300, message = "Descripción máximo 300 caracteres")
        private String descripcion;

        private Boolean activo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String codigo;
        private String nombre;
        private String tipo;
        private Integer velocidadMbps;
        private BigDecimal precioBase;
        private String descripcion;
        private Boolean activo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
