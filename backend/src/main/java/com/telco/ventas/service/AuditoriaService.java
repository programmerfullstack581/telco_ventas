package com.telco.ventas.service;

import com.telco.ventas.dto.AuditoriaDto;
import com.telco.ventas.entity.Auditoria;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    @Transactional
    public void registrar(String accion, String entidad, Long entidadId, String detalle, Usuario usuario) {
        Auditoria auditoria = Auditoria.builder()
                .accion(accion)
                .entidad(entidad)
                .entidadId(entidadId)
                .detalle(detalle == null || detalle.length() <= 500 ? detalle : detalle.substring(0, 500))
                .usuarioId(usuario != null ? usuario.getId() : null)
                .usuarioUsername(usuario != null ? usuario.getUsername() : null)
                .fecha(LocalDateTime.now())
                .build();
        auditoriaRepository.save(auditoria);
    }

    public Page<AuditoriaDto> listar(String accion, String username,
                                     LocalDateTime desde, LocalDateTime hasta,
                                     Pageable pageable) {
        String a = accion == null ? "" : accion;
        String u = username == null ? "" : username;

        Page<Auditoria> page;
        if (desde != null && hasta != null) {
            page = auditoriaRepository
                    .findByAccionContainingIgnoreCaseAndUsuarioUsernameContainingIgnoreCaseAndFechaBetween(a, u, desde, hasta, pageable);
        } else {
            page = auditoriaRepository.findByAccionContainingIgnoreCaseAndUsuarioUsernameContainingIgnoreCase(a, u, pageable);
        }

        List<AuditoriaDto> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    private AuditoriaDto toResponse(Auditoria auditoria) {
        return AuditoriaDto.builder()
                .id(auditoria.getId())
                .usuarioId(auditoria.getUsuarioId())
                .usuarioUsername(auditoria.getUsuarioUsername())
                .accion(auditoria.getAccion())
                .entidad(auditoria.getEntidad())
                .entidadId(auditoria.getEntidadId())
                .detalle(auditoria.getDetalle())
                .fecha(auditoria.getFecha())
                .build();
    }
}
