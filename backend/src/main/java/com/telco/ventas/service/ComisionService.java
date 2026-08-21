package com.telco.ventas.service;

import com.telco.ventas.dto.ComisionDto;
import com.telco.ventas.entity.Comision;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.exception.BusinessException;
import com.telco.ventas.exception.ResourceNotFoundException;
import com.telco.ventas.repository.ComisionRepository;
import com.telco.ventas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComisionService {

    private final ComisionRepository comisionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public List<ComisionDto> listar(List<Long> agenteIds) {
        List<Comision> comisiones = agenteIds == null || agenteIds.isEmpty()
                ? comisionRepository.findAll()
                : comisionRepository.findByAgenteIdInOrderByFechaCalculoDesc(agenteIds);
        return comisiones.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ComisionDto marcarPagada(Long id, Usuario usuario) {
        Comision comision = comisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comisión no encontrada: " + id));
        if ("PAGADA".equals(comision.getEstado())) {
            throw new BusinessException("La comisión ya se encuentra PAGADA");
        }
        comision.setEstado("PAGADA");
        comision.setFechaPago(LocalDateTime.now());
        comision = comisionRepository.save(comision);
        auditoriaService.registrar("PAGAR_COMISION", "COMISION", comision.getId(),
                "Comisión pagada por venta " + comision.getVentaId(), usuario);
        return toResponse(comision);
    }

    private ComisionDto toResponse(Comision comision) {
        String username = usuarioRepository.findById(comision.getAgenteId())
                .map(Usuario::getUsername).orElse(null);
        return ComisionDto.builder()
                .id(comision.getId())
                .ventaId(comision.getVentaId())
                .agenteId(comision.getAgenteId())
                .agenteUsername(username)
                .montoBase(comision.getMontoBase())
                .porcentaje(comision.getPorcentaje())
                .montoComision(comision.getMontoComision())
                .estado(comision.getEstado())
                .fechaCalculo(comision.getFechaCalculo())
                .fechaPago(comision.getFechaPago())
                .build();
    }
}
