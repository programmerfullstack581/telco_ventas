package com.telco.ventas.service;

import com.telco.ventas.dto.CreateVentaRequest;
import com.telco.ventas.dto.RechazarVentaRequest;
import com.telco.ventas.dto.ResumenVentasResponse;
import com.telco.ventas.dto.VentaHistorialDto;
import com.telco.ventas.dto.VentaResponse;
import com.telco.ventas.entity.Cliente;
import com.telco.ventas.entity.Comision;
import com.telco.ventas.entity.EstadoVenta;
import com.telco.ventas.entity.Plan;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.entity.Venta;
import com.telco.ventas.entity.VentaHistorial;
import com.telco.ventas.exception.BusinessException;
import com.telco.ventas.exception.ResourceNotFoundException;
import com.telco.ventas.repository.ComisionRepository;
import com.telco.ventas.repository.ClienteRepository;
import com.telco.ventas.repository.PlanRepository;
import com.telco.ventas.repository.UsuarioRepository;
import com.telco.ventas.repository.VentaHistorialRepository;
import com.telco.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanRepository planRepository;
    private final ClienteRepository clienteRepository;
    private final VentaHistorialRepository historialRepository;
    private final ComisionRepository comisionRepository;
    private final EquipoService equipoService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public VentaResponse crearVenta(CreateVentaRequest request, Usuario agente) {
        if (ventaRepository.existsByCodigoLlamada(request.getCodigoLlamada())) {
            throw new BusinessException("Código de llamada ya existe: " + request.getCodigoLlamada());
        }

        Cliente cliente = clienteRepository.findByDni(request.getDniCliente()).orElse(null);
        if (cliente == null) {
            cliente = Cliente.builder()
                    .dni(request.getDniCliente())
                    .nombreCliente(request.getNombreCliente())
                    .telefono(request.getTelefonoCliente())
                    .direccion(request.getDireccionCliente())
                    .distritoId(request.getDistritoId())
                    .email(request.getEmail())
                    .activo(true)
                    .build();
            cliente = clienteRepository.save(cliente);
            auditoriaService.registrar("CREAR_CLIENTE", "CLIENTE", cliente.getId(),
                    "Cliente creado automáticamente: " + cliente.getDni(), agente);
        }

        Plan plan = request.getPlanId() == null ? null
                : planRepository.findById(request.getPlanId())
                        .filter(Plan::getActivo)
                        .orElse(null);

        Venta venta = Venta.builder()
                .agenteId(agente.getId())
                .dniCliente(request.getDniCliente())
                .nombreCliente(request.getNombreCliente())
                .telefonoCliente(request.getTelefonoCliente())
                .direccionCliente(request.getDireccionCliente())
                .planActual(request.getPlanActual())
                .planNuevo(request.getPlanNuevo())
                .codigoLlamada(request.getCodigoLlamada())
                .producto(request.getProducto())
                .monto(request.getMonto())
                .estado(EstadoVenta.PENDIENTE)
                .fechaRegistro(LocalDateTime.now())
                .clienteId(cliente.getId())
                .planId(plan != null ? plan.getId() : null)
                .build();

        venta = ventaRepository.save(venta);

        registrarHistorial(venta.getId(), null, EstadoVenta.PENDIENTE.name(), agente, null);
        auditoriaService.registrar("CREAR_VENTA", "VENTA", venta.getId(),
                "Venta " + venta.getCodigoLlamada() + " por " + venta.getNombreCliente(), agente);

        return toResponse(venta);
    }

    public Page<VentaResponse> misVentas(Long agenteId, EstadoVenta estado,
                                         LocalDateTime desde, LocalDateTime hasta,
                                         Pageable pageable) {
        Page<Venta> page;
        if (estado != null && desde != null && hasta != null) {
            page = ventaRepository.findByAgenteIdAndEstadoAndFechaRegistroBetween(agenteId, estado, desde, hasta, pageable);
        } else if (estado != null) {
            page = ventaRepository.findByAgenteIdAndEstado(agenteId, estado, pageable);
        } else if (desde != null && hasta != null) {
            page = ventaRepository.findByAgenteIdAndFechaRegistroBetween(agenteId, desde, hasta, pageable);
        } else {
            page = ventaRepository.findByAgenteId(agenteId, pageable);
        }
        List<VentaResponse> content = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public Page<VentaResponse> ventasPendientes(Pageable pageable) {
        Page<Venta> page = ventaRepository.findByEstado(EstadoVenta.PENDIENTE, pageable);
        List<VentaResponse> content = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional
    public VentaResponse aprobarVenta(Long ventaId, Usuario validador) {
        Venta venta = getVenta(ventaId);
        if (venta.getEstado() != EstadoVenta.PENDIENTE) {
            throw new BusinessException("La venta no se encuentra en estado PENDIENTE");
        }
        String estadoAnterior = venta.getEstado().name();
        venta.setEstado(EstadoVenta.APROBADA);
        venta.setFechaValidacion(LocalDateTime.now());
        venta = ventaRepository.save(venta);

        registrarHistorial(venta.getId(), estadoAnterior, EstadoVenta.APROBADA.name(), validador, null);
        auditoriaService.registrar("APROBAR_VENTA", "VENTA", venta.getId(),
                "Venta " + venta.getCodigoLlamada() + " aprobada por " + validador.getUsername(), validador);

        generarComision(venta, validador);
        return toResponse(venta);
    }

    @Transactional
    public VentaResponse rechazarVenta(Long ventaId, RechazarVentaRequest request, Usuario validador) {
        Venta venta = getVenta(ventaId);
        if (venta.getEstado() != EstadoVenta.PENDIENTE) {
            throw new BusinessException("La venta no se encuentra en estado PENDIENTE");
        }
        String estadoAnterior = venta.getEstado().name();
        venta.setEstado(EstadoVenta.RECHAZADA);
        venta.setMotivoRechazo(request.getMotivoRechazo());
        venta.setFechaValidacion(LocalDateTime.now());
        venta = ventaRepository.save(venta);

        registrarHistorial(venta.getId(), estadoAnterior, EstadoVenta.RECHAZADA.name(), validador, request.getMotivoRechazo());
        auditoriaService.registrar("RECHAZAR_VENTA", "VENTA", venta.getId(),
                "Venta " + venta.getCodigoLlamada() + " rechazada por " + validador.getUsername(), validador);

        return toResponse(venta);
    }

    public List<VentaResponse> ventasEquipo(Usuario usuario, EstadoVenta estado,
                                            Long agenteId,
                                            LocalDateTime desde, LocalDateTime hasta) {
        List<Long> agenteIds = equipoService.resolverAgenteIds(usuario);

        if (agenteId != null) {
            if (!agenteIds.contains(agenteId)) {
                throw new BusinessException("El agente no pertenece a su equipo");
            }
            agenteIds = List.of(agenteId);
        }

        List<Venta> ventas;
        if (estado != null && desde != null && hasta != null) {
            ventas = ventaRepository.findByAgenteIdInAndEstadoAndFechaRegistroBetween(agenteIds, estado, desde, hasta);
        } else if (estado != null) {
            ventas = ventaRepository.findByAgenteIdInAndEstado(agenteIds, estado);
        } else if (desde != null && hasta != null) {
            ventas = ventaRepository.findByAgenteIdInAndFechaRegistroBetween(agenteIds, desde, hasta);
        } else {
            ventas = ventaRepository.findByAgenteIdIn(agenteIds);
        }
        return ventas.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<VentaHistorialDto> historialVenta(Long ventaId) {
        getVenta(ventaId);
        return historialRepository.findByVentaIdOrderByFechaAsc(ventaId).stream()
                .map(h -> {
                    String username = h.getUsuarioId() == null ? null
                            : usuarioRepository.findById(h.getUsuarioId()).map(Usuario::getUsername).orElse(null);
                    return VentaHistorialDto.builder()
                            .id(h.getId())
                            .ventaId(h.getVentaId())
                            .estadoAnterior(h.getEstadoAnterior())
                            .estadoNuevo(h.getEstadoNuevo())
                            .usuarioId(h.getUsuarioId())
                            .usuarioUsername(username)
                            .motivo(h.getMotivo())
                            .fecha(h.getFecha())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void registrarHistorial(Long ventaId, String estadoAnterior, String estadoNuevo,
                                    Usuario usuario, String motivo) {
        historialRepository.save(VentaHistorial.builder()
                .ventaId(ventaId)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .usuarioId(usuario != null ? usuario.getId() : null)
                .motivo(motivo)
                .fecha(LocalDateTime.now())
                .build());
    }

    private void generarComision(Venta venta, Usuario validador) {
        if (comisionRepository.findByVentaId(venta.getId()).isPresent()) {
            return;
        }
        BigDecimal porcentaje = new BigDecimal("5.00");
        BigDecimal montoComision = venta.getMonto().multiply(porcentaje)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        Comision comision = Comision.builder()
                .ventaId(venta.getId())
                .agenteId(venta.getAgenteId())
                .montoBase(venta.getMonto())
                .porcentaje(porcentaje)
                .montoComision(montoComision)
                .estado("PENDIENTE")
                .build();
        comision = comisionRepository.save(comision);
        auditoriaService.registrar("GENERAR_COMISION", "COMISION", comision.getId(),
                "Comisión generada por venta " + venta.getCodigoLlamada() + " (S/ " + montoComision + ")", validador);
    }

    private Venta getVenta(Long ventaId) {
        return ventaRepository.findById(ventaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + ventaId));
    }

    private VentaResponse toResponse(Venta venta) {
        String agenteUsername = usuarioRepository.findById(venta.getAgenteId())
                .map(Usuario::getUsername)
                .orElse(null);
        String planCodigo = venta.getPlanId() == null ? null
                : planRepository.findById(venta.getPlanId()).map(Plan::getCodigo).orElse(null);

        return VentaResponse.builder()
                .id(venta.getId())
                .agenteId(venta.getAgenteId())
                .agenteUsername(agenteUsername)
                .dniCliente(venta.getDniCliente())
                .nombreCliente(venta.getNombreCliente())
                .telefonoCliente(venta.getTelefonoCliente())
                .direccionCliente(venta.getDireccionCliente())
                .planActual(venta.getPlanActual())
                .planNuevo(venta.getPlanNuevo())
                .codigoLlamada(venta.getCodigoLlamada())
                .producto(venta.getProducto())
                .monto(venta.getMonto())
                .estado(venta.getEstado())
                .motivoRechazo(venta.getMotivoRechazo())
                .fechaRegistro(venta.getFechaRegistro())
                .fechaValidacion(venta.getFechaValidacion())
                .clienteId(venta.getClienteId())
                .planId(venta.getPlanId())
                .planCodigo(planCodigo)
                .createdAt(venta.getCreatedAt())
                .updatedAt(venta.getUpdatedAt())
                .build();
    }
}
