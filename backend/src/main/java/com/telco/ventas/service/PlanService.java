package com.telco.ventas.service;

import com.telco.ventas.dto.PlanDto;
import com.telco.ventas.entity.Plan;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.exception.BusinessException;
import com.telco.ventas.exception.ResourceNotFoundException;
import com.telco.ventas.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final AuditoriaService auditoriaService;

    public List<PlanDto.Response> listarActivos() {
        return planRepository.findByActivoTrueOrderByTipoAscNombreAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PlanDto.Response> listarTodos() {
        return planRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PlanDto.Response crear(PlanDto.Request request, Usuario usuario) {
        if (planRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("El código de plan ya existe: " + request.getCodigo());
        }
        Plan plan = Plan.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .tipo(request.getTipo())
                .velocidadMbps(request.getVelocidadMbps())
                .precioBase(request.getPrecioBase())
                .descripcion(request.getDescripcion())
                .activo(request.getActivo() == null ? true : request.getActivo())
                .build();
        plan = planRepository.save(plan);
        auditoriaService.registrar("CREAR_PLAN", "PLAN", plan.getId(),
                "Plan creado: " + plan.getCodigo() + " - " + plan.getNombre(), usuario);
        return toResponse(plan);
    }

    @Transactional
    public PlanDto.Response actualizar(Long id, PlanDto.Request request, Usuario usuario) {
        Plan plan = getPlan(id);
        plan.setCodigo(request.getCodigo());
        plan.setNombre(request.getNombre());
        plan.setTipo(request.getTipo());
        plan.setVelocidadMbps(request.getVelocidadMbps());
        plan.setPrecioBase(request.getPrecioBase());
        plan.setDescripcion(request.getDescripcion());
        if (request.getActivo() != null) {
            plan.setActivo(request.getActivo());
        }
        plan = planRepository.save(plan);
        auditoriaService.registrar("EDITAR_PLAN", "PLAN", plan.getId(),
                "Plan actualizado: " + plan.getCodigo(), usuario);
        return toResponse(plan);
    }

    @Transactional
    public void cambiarEstado(Long id, Boolean activo, Usuario usuario) {
        Plan plan = getPlan(id);
        plan.setActivo(activo);
        planRepository.save(plan);
        auditoriaService.registrar(activo ? "ACTIVAR_PLAN" : "DESACTIVAR_PLAN", "PLAN", plan.getId(),
                "Plan " + plan.getCodigo() + " -> " + (activo ? "activo" : "inactivo"), usuario);
    }

    public PlanDto.Response obtener(Long id) {
        return toResponse(getPlan(id));
    }

    public Plan getPlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado: " + id));
    }

    private PlanDto.Response toResponse(Plan plan) {
        return PlanDto.Response.builder()
                .id(plan.getId())
                .codigo(plan.getCodigo())
                .nombre(plan.getNombre())
                .tipo(plan.getTipo())
                .velocidadMbps(plan.getVelocidadMbps())
                .precioBase(plan.getPrecioBase())
                .descripcion(plan.getDescripcion())
                .activo(plan.getActivo())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
