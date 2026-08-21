package com.telco.ventas.service;

import com.telco.ventas.entity.Usuario;
import com.telco.ventas.repository.RolRepository;
import com.telco.ventas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipoService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public List<Long> resolverAgenteIds(Usuario usuario) {
        String rol = usuarioRepository.findRolNombreByUsuarioId(usuario.getId()).orElse("");
        if (rol.equals("ADMIN") || rol.equals("BACKOFFICE")) {
            Long rolAgenteId = rolRepository.findByNombre("AGENTE").map(u -> u.getId()).orElse(null);
            return rolAgenteId == null ? List.of() : usuarioRepository.findByRolId(rolAgenteId).stream()
                    .map(Usuario::getId)
                    .collect(Collectors.toList());
        }
        return usuarioRepository.findBySupervisorId(usuario.getId()).stream()
                .map(Usuario::getId)
                .collect(Collectors.toList());
    }

    public boolean esAgenteDelSupervisor(Long supervisorId, Long agenteId) {
        return usuarioRepository.findBySupervisorId(supervisorId).stream()
                .anyMatch(u -> u.getId().equals(agenteId));
    }
}
