package com.telco.ventas.service;

import com.telco.ventas.dto.ClienteDto;
import com.telco.ventas.entity.Cliente;
import com.telco.ventas.entity.Distrito;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.exception.BusinessException;
import com.telco.ventas.exception.ResourceNotFoundException;
import com.telco.ventas.repository.ClienteRepository;
import com.telco.ventas.repository.DistritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final DistritoRepository distritoRepository;
    private final AuditoriaService auditoriaService;

    public Page<ClienteDto.Response> listar(String search, Pageable pageable) {
        String termino = search == null ? "" : search.trim();
        Page<Cliente> page;
        if (termino.isEmpty()) {
            page = clienteRepository.findAll(pageable);
        } else {
            page = clienteRepository
                    .findByDniContainingIgnoreCaseOrNombreClienteContainingIgnoreCase(termino, termino, pageable);
        }
        List<ClienteDto.Response> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public ClienteDto.Response buscarPorDni(String dni) {
        return clienteRepository.findByDni(dni).map(this::toResponse).orElse(null);
    }

    public ClienteDto.Response obtener(Long id) {
        return toResponse(getCliente(id));
    }

    @Transactional
    public ClienteDto.Response crear(ClienteDto.Request request, Usuario usuario) {
        if (clienteRepository.existsByDni(request.getDni())) {
            throw new BusinessException("El cliente con DNI " + request.getDni() + " ya existe");
        }
        Cliente cliente = Cliente.builder()
                .dni(request.getDni())
                .nombreCliente(request.getNombreCliente())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .distritoId(request.getDistritoId())
                .email(request.getEmail())
                .activo(request.getActivo() == null ? true : request.getActivo())
                .build();
        cliente = clienteRepository.save(cliente);
        auditoriaService.registrar("CREAR_CLIENTE", "CLIENTE", cliente.getId(),
                "Cliente creado: " + cliente.getDni() + " - " + cliente.getNombreCliente(), usuario);
        return toResponse(cliente);
    }

    @Transactional
    public ClienteDto.Response actualizar(Long id, ClienteDto.Request request, Usuario usuario) {
        Cliente cliente = getCliente(id);
        cliente.setNombreCliente(request.getNombreCliente());
        cliente.setTelefono(request.getTelefono());
        cliente.setDireccion(request.getDireccion());
        cliente.setDistritoId(request.getDistritoId());
        cliente.setEmail(request.getEmail());
        if (request.getActivo() != null) {
            cliente.setActivo(request.getActivo());
        }
        cliente = clienteRepository.save(cliente);
        auditoriaService.registrar("EDITAR_CLIENTE", "CLIENTE", cliente.getId(),
                "Cliente actualizado: " + cliente.getDni(), usuario);
        return toResponse(cliente);
    }

    public Cliente getCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private ClienteDto.Response toResponse(Cliente cliente) {
        Distrito distrito = cliente.getDistritoId() == null ? null
                : distritoRepository.findById(cliente.getDistritoId()).orElse(null);
        return ClienteDto.Response.builder()
                .id(cliente.getId())
                .dni(cliente.getDni())
                .nombreCliente(cliente.getNombreCliente())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .distritoId(cliente.getDistritoId())
                .distritoNombre(distrito != null ? distrito.getNombre() : null)
                .departamento(distrito != null ? distrito.getDepartamento() : null)
                .email(cliente.getEmail())
                .fechaRegistro(cliente.getFechaRegistro())
                .activo(cliente.getActivo())
                .createdAt(cliente.getCreatedAt())
                .updatedAt(cliente.getUpdatedAt())
                .build();
    }
}
