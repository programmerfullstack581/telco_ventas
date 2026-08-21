package com.telco.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenVentasResponse {
    private Long totalVentas;
    private Long totalPendientes;
    private Long totalAprobadas;
    private Long totalRechazadas;
    private BigDecimal montoTotalAprobadas;
    private List<VentasPorDia> ventasPorDia;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentasPorDia {
        private LocalDate fecha;
        private Long cantidad;
        private BigDecimal monto;
    }
}
