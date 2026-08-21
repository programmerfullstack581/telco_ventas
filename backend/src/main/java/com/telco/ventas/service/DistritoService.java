package com.telco.ventas.service;

import com.telco.ventas.dto.DistritoDto;
import com.telco.ventas.entity.Distrito;
import com.telco.ventas.repository.DistritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DistritoService {

    private final DistritoRepository distritoRepository;

    public List<DistritoDto.Response> listar() {
        return distritoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private DistritoDto.Response toResponse(Distrito distrito) {
        return DistritoDto.Response.builder()
                .id(distrito.getId())
                .nombre(distrito.getNombre())
                .provincia(distrito.getProvincia())
                .departamento(distrito.getDepartamento())
                .codigoUbigeo(distrito.getCodigoUbigeo())
                .activo(distrito.getActivo())
                .build();
    }
}
