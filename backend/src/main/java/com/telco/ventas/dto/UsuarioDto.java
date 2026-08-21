package com.telco.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UsuarioDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Username es obligatorio")
        @Size(max = 50, message = "Username máximo 50 caracteres")
        private String username;

        @NotBlank(message = "Contraseña es obligatoria")
        @Size(min = 6, message = "Contraseña mínimo 6 caracteres")
        private String password;

        @NotBlank(message = "Rol es obligatorio")
        private String rol;

        private Long supervisorId;

        private Boolean activo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String password;
        private String rol;
        private Long supervisorId;
        private Boolean activo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String username;
        private String rol;
        private Long rolId;
        private Long supervisorId;
        private String supervisorUsername;
        private Boolean activo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
