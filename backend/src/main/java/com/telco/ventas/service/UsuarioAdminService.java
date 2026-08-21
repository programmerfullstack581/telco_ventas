package com.telco.ventas.service;

import com.telco.ventas.dto.UsuarioDto;
import com.telco.ventas.entity.Rol;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.exception.BusinessException;
import com.telco.ventas.exception.ResourceNotFoundException;
import com.telco.ventas.repository.AuditoriaRepository;
import com.telco.ventas.repository.ComisionRepository;
import com.telco.ventas.repository.RolRepository;
import com.telco.ventas.repository.UsuarioRepository;
import com.telco.ventas.repository.VentaHistorialRepository;
import com.telco.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final VentaRepository ventaRepository;
    private final VentaHistorialRepository ventaHistorialRepository;
    private final ComisionRepository comisionRepository;
    private final AuditoriaRepository auditoriaRepository;

    public List<UsuarioDto.Response> listar() {
        return usuarioRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UsuarioDto.Response> listarAgentesYSupervisores() {
        return usuarioRepository.findAgentesYSupervisores().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioDto.Response crear(UsuarioDto.Request request, Usuario quien) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("El username ya existe: " + request.getUsername());
        }
        Rol rol = validarRol(request.getRol());
        validarSupervisor(request.getSupervisorId(), rol);

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rolId(rol.getId())
                .supervisorId(request.getSupervisorId())
                .activo(request.getActivo() == null ? true : request.getActivo())
                .build();
        usuario = usuarioRepository.save(usuario);
        auditoriaService.registrar("CREAR_USUARIO", "USUARIO", usuario.getId(),
                "Usuario creado: " + usuario.getUsername() + " (" + rol.getNombre() + ")", quien);
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioDto.Response actualizar(Long id, UsuarioDto.UpdateRequest request, Usuario quien) {
        Usuario usuario = getUsuario(id);
        if (request.getRol() != null) {
            Rol rol = validarRol(request.getRol());
            if (id.equals(quien.getId()) && !rol.getId().equals(quien.getRolId())
                    && esRolAdmin(usuario.getRolId())) {
                throw new BusinessException("No puedes cambiar el rol ADMIN de tu propia cuenta");
            }
            usuario.setRolId(rol.getId());
        }
        if (request.getSupervisorId() != null) {
            validarSupervisor(request.getSupervisorId(), rolDelUsuario(usuario));
            usuario.setSupervisorId(request.getSupervisorId());
        } else if (esRolAgente(rolDelUsuario(usuario))) {
            validarSupervisor(usuario.getSupervisorId(), rolDelUsuario(usuario));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 6) {
                throw new BusinessException("Contraseña mínimo 6 caracteres");
            }
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getActivo() != null) {
            if (id.equals(quien.getId()) && !request.getActivo()) {
                throw new BusinessException("No puedes inhabilitar tu propio usuario");
            }
            usuario.setActivo(request.getActivo());
        }
        usuario = usuarioRepository.save(usuario);
        auditoriaService.registrar("EDITAR_USUARIO", "USUARIO", usuario.getId(),
                "Usuario actualizado: " + usuario.getUsername(), quien);
        return toResponse(usuario);
    }

    public UsuarioDto.Response obtener(Long id) {
        return toResponse(getUsuario(id));
    }

    @Transactional
    public UsuarioDto.Response cambiarEstado(Long id, Boolean activo, Usuario quien) {
        Usuario usuario = getUsuario(id);
        boolean nuevo = activo != null ? activo : !usuario.getActivo();
        if (id.equals(quien.getId()) && !nuevo) {
            throw new BusinessException("No puedes inhabilitar tu propio usuario");
        }
        usuario.setActivo(nuevo);
        usuario = usuarioRepository.save(usuario);
        auditoriaService.registrar(nuevo ? "HABILITAR_USUARIO" : "INHABILITAR_USUARIO", "USUARIO", usuario.getId(),
                "Usuario " + (nuevo ? "habilitado: " : "inhabilitado: ") + usuario.getUsername(), quien);
        return toResponse(usuario);
    }

    @Transactional
    public void eliminar(Long id, Usuario quien) {
        if (id.equals(quien.getId())) {
            throw new BusinessException("No puedes eliminar tu propio usuario");
        }
        Usuario usuario = getUsuario(id);
        usuarioRepository.findBySupervisorId(id).forEach(a -> {
            a.setSupervisorId(null);
            usuarioRepository.save(a);
        });
        comisionRepository.deleteByAgenteId(id);
        ventaHistorialRepository.deleteByUsuarioId(id);
        ventaRepository.deleteByAgenteId(id);
        auditoriaRepository.deleteByUsuarioId(id);
        auditoriaService.registrar("ELIMINAR_USUARIO", "USUARIO", id,
                "Usuario eliminado de la base de datos: " + usuario.getUsername(), quien);
        usuarioRepository.delete(usuario);
    }

    private Usuario getUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    private Rol validarRol(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("Rol es obligatorio");
        }
        Rol rol = rolRepository.findByNombre(nombre.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("Rol inválido o inexistente: " + nombre));
        if (!Boolean.TRUE.equals(rol.getActivo())) {
            throw new BusinessException("El rol está desactivado: " + rol.getNombre());
        }
        return rol;
    }

    private Rol rolDelUsuario(Usuario usuario) {
        if (usuario.getRolId() == null) {
            return null;
        }
        return rolRepository.findById(usuario.getRolId()).orElse(null);
    }

    private boolean esRolAgente(Rol rol) {
        return rol != null && rol.getNombre().equals("AGENTE");
    }

    private boolean esRolAdmin(Long rolId) {
        return rolRepository.findById(rolId).map(r -> r.getNombre().equals("ADMIN")).orElse(false);
    }

    private void validarSupervisor(Long supervisorId, Rol rol) {
        if (esRolAgente(rol)) {
            if (supervisorId == null) {
                throw new BusinessException("Un AGENTE debe tener un supervisor asignado");
            }
            Usuario sup = getUsuario(supervisorId);
            Rol rolSup = rolDelUsuario(sup);
            if (rolSup == null || !rolSup.getNombre().equals("SUPERVISOR")) {
                throw new BusinessException("El supervisor asignado debe tener rol SUPERVISOR");
            }
        }
    }

    private UsuarioDto.Response toResponse(Usuario usuario) {
        String rolNombre = usuario.getRolId() == null ? "SIN_ROL"
                : rolRepository.findById(usuario.getRolId()).map(Rol::getNombre).orElse("SIN_ROL");
        String supUsername = usuario.getSupervisorId() == null ? null
                : usuarioRepository.findById(usuario.getSupervisorId())
                        .map(Usuario::getUsername).orElse(null);
        return UsuarioDto.Response.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .rol(rolNombre)
                .rolId(usuario.getRolId())
                .supervisorId(usuario.getSupervisorId())
                .supervisorUsername(supUsername)
                .activo(usuario.getActivo())
                .createdAt(usuario.getCreatedAt())
                .updatedAt(usuario.getUpdatedAt())
                .build();
    }
}
