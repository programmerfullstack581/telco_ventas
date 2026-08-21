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
public class VentaHistorialDto {
    private Long id;
    private Long ventaId;
    private String estadoAnterior;
    private String estadoNuevo;
    private Long usuarioId;
    private String usuarioUsername;
    private String motivo;
    private LocalDateTime fecha;
}
