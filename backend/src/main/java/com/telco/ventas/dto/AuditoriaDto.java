package com.telco.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaDto {
    private Long id;
    private Long usuarioId;
    private String usuarioUsername;
    private String accion;
    private String entidad;
    private Long entidadId;
    private String detalle;
    private LocalDateTime fecha;
}
