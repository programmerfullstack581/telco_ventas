package com.telco.ventas.service;

import com.telco.ventas.dto.ReporteDto;
import com.telco.ventas.dto.ResumenVentasResponse;
import com.telco.ventas.entity.EstadoVenta;
import com.telco.ventas.entity.Plan;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.repository.ComisionRepository;
import com.telco.ventas.repository.PlanRepository;
import com.telco.ventas.repository.UsuarioRepository;
import com.telco.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanRepository planRepository;
    private final ComisionRepository comisionRepository;
    private final EquipoService equipoService;

    public ResumenVentasResponse resumenVentas(Usuario usuario,
                                               LocalDate dia,
                                               Integer anio, Integer mes,
                                               LocalDate desde, LocalDate hasta) {
        List<Long> agenteIds = equipoService.resolverAgenteIds(usuario);
        LocalDateTime[] rango = resolverRango(dia, anio, mes, desde, hasta);
        LocalDateTime fechaDesde = rango[0];
        LocalDateTime fechaHasta = rango[1];

        Long pendientes = ventaRepository.countByAgenteIdInAndEstado(agenteIds, EstadoVenta.PENDIENTE);
        Long aprobadas = ventaRepository.countByAgenteIdInAndEstado(agenteIds, EstadoVenta.APROBADA);
        Long rechazadas = ventaRepository.countByAgenteIdInAndEstado(agenteIds, EstadoVenta.RECHAZADA);
        BigDecimal montoTotal = ventaRepository.sumMontoAprobadas(agenteIds);

        List<Object[]> porDia = ventaRepository.ventasPorDia(agenteIds, fechaDesde, fechaHasta);
        List<ResumenVentasResponse.VentasPorDia> ventasPorDias = porDia.stream()
                .map(row -> ResumenVentasResponse.VentasPorDia.builder()
                        .fecha(((Date) row[0]).toLocalDate())
                        .cantidad(((Number) row[1]).longValue())
                        .monto((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        return ResumenVentasResponse.builder()
                .totalVentas(pendientes + aprobadas + rechazadas)
                .totalPendientes(pendientes)
                .totalAprobadas(aprobadas)
                .totalRechazadas(rechazadas)
                .montoTotalAprobadas(montoTotal)
                .ventasPorDia(ventasPorDias)
                .build();
    }

    public List<ReporteDto.PorPlan> ventasPorPlan(Usuario usuario,
                                                  LocalDate dia, Integer anio, Integer mes,
                                                  LocalDate desde, LocalDate hasta) {
        List<Long> agenteIds = equipoService.resolverAgenteIds(usuario);
        LocalDateTime[] rango = resolverRango(dia, anio, mes, desde, hasta);

        Map<Long, Plan> planMap = new HashMap<>();
        planRepository.findAll().forEach(p -> planMap.put(p.getId(), p));

        return ventaRepository.ventasPorPlan(agenteIds, rango[0], rango[1]).stream()
                .map(row -> {
                    Long planId = ((Number) row[0]).longValue();
                    Plan plan = planId > 0 ? planMap.get(planId) : null;
                    return ReporteDto.PorPlan.builder()
                            .planId(planId > 0 ? planId : null)
                            .planCodigo(plan != null ? plan.getCodigo() : "SIN PLAN")
                            .planNombre(plan != null ? plan.getNombre() : "Sin plan asignado")
                            .cantidad(((Number) row[1]).longValue())
                            .aprobadas(((Number) row[2]).longValue())
                            .montoAprobado((BigDecimal) row[3])
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<ReporteDto.PorAgente> ventasPorAgente(Usuario usuario,
                                                      LocalDate dia, Integer anio, Integer mes,
                                                      LocalDate desde, LocalDate hasta) {
        List<Long> agenteIds = equipoService.resolverAgenteIds(usuario);
        LocalDateTime[] rango = resolverRango(dia, anio, mes, desde, hasta);

        Map<Long, String> usernameMap = new HashMap<>();
        usuarioRepository.findAll().forEach(u -> usernameMap.put(u.getId(), u.getUsername()));

        return ventaRepository.ventasPorAgente(agenteIds, rango[0], rango[1]).stream()
                .map(row -> {
                    Long agenteId = ((Number) row[0]).longValue();
                    return ReporteDto.PorAgente.builder()
                            .agenteId(agenteId)
                            .agenteUsername(usernameMap.getOrDefault(agenteId, "Agente " + agenteId))
                            .total(((Number) row[1]).longValue())
                            .pendientes(((Number) row[2]).longValue())
                            .aprobadas(((Number) row[3]).longValue())
                            .rechazadas(((Number) row[4]).longValue())
                            .montoAprobado((BigDecimal) row[5])
                            .build();
                })
                .collect(Collectors.toList());
    }

    public ReporteDto.ResumenComisiones resumenComisiones(Usuario usuario) {
        List<Long> agenteIds = equipoService.resolverAgenteIds(usuario);

        Map<Long, String> usernameMap = new HashMap<>();
        usuarioRepository.findAll().forEach(u -> usernameMap.put(u.getId(), u.getUsername()));

        List<ReporteDto.ComisionAgente> porAgente = comisionRepository.resumenPorAgente(agenteIds).stream()
                .map(row -> {
                    Long agenteId = ((Number) row[0]).longValue();
                    return ReporteDto.ComisionAgente.builder()
                            .agenteId(agenteId)
                            .agenteUsername(usernameMap.getOrDefault(agenteId, "Agente " + agenteId))
                            .total(((Number) row[1]).longValue())
                            .pendientes(((Number) row[2]).longValue())
                            .pagadas(((Number) row[3]).longValue())
                            .montoPendiente((BigDecimal) row[4])
                            .montoPagado((BigDecimal) row[5])
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalPendiente = comisionRepository.sumPendientes(agenteIds);
        BigDecimal totalPagado = comisionRepository.sumPagadas(agenteIds);

        return ReporteDto.ResumenComisiones.builder()
                .totalPendiente(totalPendiente)
                .totalPagado(totalPagado)
                .porAgente(porAgente)
                .build();
    }

    private LocalDateTime[] resolverRango(LocalDate dia, Integer anio, Integer mes,
                                          LocalDate desde, LocalDate hasta) {
        LocalDateTime fechaDesde;
        LocalDateTime fechaHasta;

        if (desde != null && hasta != null) {
            fechaDesde = desde.atStartOfDay();
            fechaHasta = hasta.atTime(LocalTime.MAX);
        } else if (dia != null) {
            fechaDesde = dia.atStartOfDay();
            fechaHasta = dia.atTime(LocalTime.MAX);
        } else if (anio != null && mes != null) {
            LocalDate firstDay = LocalDate.of(anio, mes, 1);
            LocalDate lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());
            fechaDesde = firstDay.atStartOfDay();
            fechaHasta = lastDay.atTime(LocalTime.MAX);
        } else {
            fechaDesde = LocalDate.now().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
            fechaHasta = LocalDate.now().with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX);
        }
        return new LocalDateTime[]{fechaDesde, fechaHasta};
    }
}
