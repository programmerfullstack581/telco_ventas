package com.telco.ventas.service;

import com.telco.ventas.dto.RolDto;
import com.telco.ventas.entity.Permiso;
import com.telco.ventas.entity.Rol;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.exception.BusinessException;
import com.telco.ventas.exception.ResourceNotFoundException;
import com.telco.ventas.repository.PermisoRepository;
import com.telco.ventas.repository.RolRepository;
import com.telco.ventas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolService {

    private static final String ROL_ADMIN = "ADMIN";

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public List<RolDto.Response> listar() {
        return rolRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<RolDto.PermisoResponse> listarPermisos() {
        return permisoRepository.findAllByOrderByModuloAscAccionAsc().stream()
                .map(p -> RolDto.PermisoResponse.builder()
                        .id(p.getId()).codigo(p.getCodigo()).modulo(p.getModulo())
                        .accion(p.getAccion()).descripcion(p.getDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public RolDto.Response crear(RolDto.Request request, Usuario quien) {
        String nombre = request.getNombre().trim().toUpperCase();
        if (rolRepository.existsByNombre(nombre)) {
            throw new BusinessException("Ya existe un rol con nombre: " + nombre);
        }
        Rol rol = Rol.builder()
                .nombre(nombre)
                .descripcion(request.getDescripcion())
                .activo(request.getActivo() == null ? true : request.getActivo())
                .permisos(new HashSet<>(resolverPermisos(request.getPermisos())))
                .build();
        rol = rolRepository.save(rol);
        auditoriaService.registrar("CREAR_ROL", "ROL", rol.getId(),
                "Rol creado: " + rol.getNombre() + " con " + rol.getPermisos().size() + " permiso(s)", quien);
        return toResponse(rol);
    }

    @Transactional
    public RolDto.Response actualizar(Long id, RolDto.Request request, Usuario quien) {
        Rol rol = getRol(id);
        if (esAdmin(rol) && (request.getNombre() == null || !request.getNombre().trim().equalsIgnoreCase(ROL_ADMIN))) {
            throw new BusinessException("El rol ADMIN no puede ser renombrado");
        }
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            String nombre = request.getNombre().trim().toUpperCase();
            if (!nombre.equals(rol.getNombre()) && rolRepository.existsByNombre(nombre)) {
                throw new BusinessException("Ya existe un rol con nombre: " + nombre);
            }
            rol.setNombre(nombre);
        }
        if (request.getDescripcion() != null) {
            rol.setDescripcion(request.getDescripcion());
        }
        if (request.getActivo() != null) {
            if (esAdmin(rol) && !request.getActivo()) {
                throw new BusinessException("El rol ADMIN no puede ser desactivado");
            }
            if (rol.getId().equals(quien.getRolId()) && !request.getActivo()) {
                throw new BusinessException("No puedes desactivar el rol de tu propia cuenta");
            }
            rol.setActivo(request.getActivo());
        }
        if (request.getPermisos() != null) {
            aplicarPermisos(rol, request.getPermisos(), quien);
        }
        rol = rolRepository.save(rol);
        auditoriaService.registrar("EDITAR_ROL", "ROL", rol.getId(),
                "Rol actualizado: " + rol.getNombre(), quien);
        return toResponse(rol);
    }

    @Transactional
    public RolDto.Response asignarPermisos(Long id, List<String> codigos, Usuario quien) {
        Rol rol = getRol(id);
        aplicarPermisos(rol, codigos, quien);
        rol = rolRepository.save(rol);
        auditoriaService.registrar("CAMBIAR_PERMISOS", "ROL", rol.getId(),
                "Permisos actualizados del rol " + rol.getNombre(), quien);
        return toResponse(rol);
    }

    @Transactional
    public RolDto.Response cambiarEstado(Long id, Boolean activo, Usuario quien) {
        Rol rol = getRol(id);
        boolean nuevo = activo != null ? activo : !rol.getActivo();
        if (esAdmin(rol) && !nuevo) {
            throw new BusinessException("El rol ADMIN no puede ser desactivado");
        }
        if (rol.getId().equals(quien.getRolId()) && !nuevo) {
            throw new BusinessException("No puedes desactivar el rol de tu propia cuenta");
        }
        rol.setActivo(nuevo);
        rol = rolRepository.save(rol);
        auditoriaService.registrar(nuevo ? "HABILITAR_ROL" : "INHABILITAR_ROL", "ROL", rol.getId(),
                "Rol " + (nuevo ? "habilitado: " : "inhabilitado: ") + rol.getNombre(), quien);
        return toResponse(rol);
    }

    @Transactional
    public void eliminar(Long id, Usuario quien) {
        Rol rol = getRol(id);
        if (esAdmin(rol)) {
            throw new BusinessException("El rol ADMIN no puede ser eliminado");
        }
        if (rol.getId().equals(quien.getRolId())) {
            throw new BusinessException("No puedes eliminar el rol de tu propia cuenta");
        }
        long asignados = usuarioRepository.countByRolId(id);
        if (asignados > 0) {
            throw new BusinessException("No se puede eliminar el rol: hay " + asignados + " usuario(s) asignado(s). Reasígnalos primero.");
        }
        rolRepository.delete(rol);
        auditoriaService.registrar("ELIMINAR_ROL", "ROL", id,
                "Rol eliminado: " + rol.getNombre(), quien);
    }

    private void aplicarPermisos(Rol rol, List<String> codigos, Usuario quien) {
        Set<String> codigoSet = codigos == null ? Set.of() : new HashSet<>(codigos);
        if (rol.getId().equals(quien.getRolId()) && !codigoSet.contains("ROLES_EDITAR")) {
            throw new BusinessException("No puedes quitar el permiso ROLES_EDITAR a tu propio rol");
        }
        if (esAdmin(rol) && !codigoSet.contains("ROLES_VER")) {
            throw new BusinessException("El rol ADMIN debe conservar el permiso ROLES_VER");
        }
        rol.setPermisos(new HashSet<>(resolverPermisos(new ArrayList<>(codigoSet))));
    }

    private List<Permiso> resolverPermisos(List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return List.of();
        }
        List<Permiso> encontrados = permisoRepository.findByCodigoIn(codigos);
        Set<String> validos = encontrados.stream().map(Permiso::getCodigo).collect(Collectors.toSet());
        for (String codigo : codigos) {
            if (!validos.contains(codigo)) {
                throw new BusinessException("Permiso inválido: " + codigo);
            }
        }
        return encontrados;
    }

    private Rol getRol(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + id));
    }

    private boolean esAdmin(Rol rol) {
        return ROL_ADMIN.equalsIgnoreCase(rol.getNombre());
    }

    private RolDto.Response toResponse(Rol rol) {
        return RolDto.Response.builder()
                .id(rol.getId())
                .nombre(rol.getNombre())
                .descripcion(rol.getDescripcion())
                .activo(rol.getActivo())
                .permisos(rol.getPermisos().stream().map(Permiso::getCodigo).collect(Collectors.toSet()))
                .usuarios(usuarioRepository.countByRolId(rol.getId()))
                .createdAt(rol.getCreatedAt())
                .updatedAt(rol.getUpdatedAt())
                .build();
    }
}
