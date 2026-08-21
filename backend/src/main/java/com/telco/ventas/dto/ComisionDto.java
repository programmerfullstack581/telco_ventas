package com.telco.ventas.dto;

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
public class ComisionDto {
    private Long id;
    private Long ventaId;
    private Long agenteId;
    private String agenteUsername;
    private BigDecimal montoBase;
    private BigDecimal porcentaje;
    private BigDecimal montoComision;
    private String estado;
    private LocalDateTime fechaCalculo;
    private LocalDateTime fechaPago;
}
