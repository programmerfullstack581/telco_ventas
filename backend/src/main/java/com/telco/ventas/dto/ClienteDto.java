package com.telco.ventas.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ClienteDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "DNI cliente es obligatorio")
        @Pattern(regexp = "^(\\d{8}|\\d{11})$", message = "DNI debe tener 8 dígitos o RUC 11 dígitos")
        private String dni;

        @NotBlank(message = "Nombre cliente es obligatorio")
        private String nombreCliente;

        @NotBlank(message = "Teléfono es obligatorio")
        @Pattern(regexp = "^\\d{9}$", message = "Teléfono debe tener 9 dígitos")
        private String telefono;

        @NotBlank(message = "Dirección es obligatoria")
        private String direccion;

        private Long distritoId;

        @Email(message = "Email inválido")
        @Size(max = 100, message = "Email máximo 100 caracteres")
        private String email;

        private Boolean activo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String dni;
        private String nombreCliente;
        private String telefono;
        private String direccion;
        private Long distritoId;
        private String distritoNombre;
        private String departamento;
        private String email;
        private LocalDateTime fechaRegistro;
        private Boolean activo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
