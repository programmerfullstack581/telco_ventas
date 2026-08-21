package com.telco.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class ReporteDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PorPlan {
        private Long planId;
        private String planCodigo;
        private String planNombre;
        private Long cantidad;
        private Long aprobadas;
        private BigDecimal montoAprobado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PorAgente {
        private Long agenteId;
        private String agenteUsername;
        private Long total;
        private Long pendientes;
        private Long aprobadas;
        private Long rechazadas;
        private BigDecimal montoAprobado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComisionAgente {
        private Long agenteId;
        private String agenteUsername;
        private Long total;
        private Long pendientes;
        private Long pagadas;
        private BigDecimal montoPendiente;
        private BigDecimal montoPagado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenComisiones {
        private BigDecimal totalPendiente;
        private BigDecimal totalPagado;
        private List<ComisionAgente> porAgente;
    }
}
