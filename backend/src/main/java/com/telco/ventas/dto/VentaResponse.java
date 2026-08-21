package com.telco.ventas.dto;

import com.telco.ventas.entity.EstadoVenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponse {
    private Long id;
    private Long agenteId;
    private String agenteUsername;
    private String dniCliente;
    private String nombreCliente;
    private String telefonoCliente;
    private String direccionCliente;
    private String planActual;
    private String planNuevo;
    private String codigoLlamada;
    private String producto;
    private BigDecimal monto;
    private EstadoVenta estado;
    private String motivoRechazo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaValidacion;
    private Long clienteId;
    private Long planId;
    private String planCodigo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
