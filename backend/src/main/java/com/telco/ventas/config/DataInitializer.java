package com.telco.ventas.config;

import com.telco.ventas.entity.Cliente;
import com.telco.ventas.entity.Comision;
import com.telco.ventas.entity.Distrito;
import com.telco.ventas.entity.EstadoVenta;
import com.telco.ventas.entity.Permiso;
import com.telco.ventas.entity.Plan;
import com.telco.ventas.entity.Rol;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.entity.Venta;
import com.telco.ventas.entity.VentaHistorial;
import com.telco.ventas.repository.ClienteRepository;
import com.telco.ventas.repository.ComisionRepository;
import com.telco.ventas.repository.DistritoRepository;
import com.telco.ventas.repository.PermisoRepository;
import com.telco.ventas.repository.PlanRepository;
import com.telco.ventas.repository.RolRepository;
import com.telco.ventas.repository.UsuarioRepository;
import com.telco.ventas.repository.VentaHistorialRepository;
import com.telco.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String[][] PERMISOS_SEED = {
            {"DASHBOARD_VER", "DASHBOARD", "VER", "Ver KPIs del panel principal"},
            {"USUARIOS_VER", "USUARIOS", "VER", "Ver listado de usuarios"},
            {"USUARIOS_CREAR", "USUARIOS", "CREAR", "Crear usuarios"},
            {"USUARIOS_EDITAR", "USUARIOS", "EDITAR", "Editar usuarios y cambiar su estado"},
            {"USUARIOS_ELIMINAR", "USUARIOS", "ELIMINAR", "Eliminar usuarios"},
            {"ROLES_VER", "ROLES", "VER", "Ver roles y permisos"},
            {"ROLES_CREAR", "ROLES", "CREAR", "Crear roles"},
            {"ROLES_EDITAR", "ROLES", "EDITAR", "Editar roles y asignar permisos"},
            {"ROLES_ELIMINAR", "ROLES", "ELIMINAR", "Eliminar roles"},
            {"PLANES_VER", "PLANES", "VER", "Ver todos los planes"},
            {"PLANES_CREAR", "PLANES", "CREAR", "Crear planes"},
            {"PLANES_EDITAR", "PLANES", "EDITAR", "Editar planes y su estado"},
            {"PLANES_ELIMINAR", "PLANES", "ELIMINAR", "Eliminar planes"},
            {"VENTAS_VER", "VENTAS", "VER", "Ver ventas"},
            {"VENTAS_CREAR", "VENTAS", "CREAR", "Registrar ventas"},
            {"VENTAS_APROBAR", "VENTAS", "APROBAR", "Aprobar ventas"},
            {"VENTAS_RECHAZAR", "VENTAS", "RECHAZAR", "Rechazar ventas"},
            {"VENTAS_ANULAR", "VENTAS", "ANULAR", "Anular ventas"},
            {"CLIENTES_VER", "CLIENTES", "VER", "Ver clientes"},
            {"CLIENTES_CREAR", "CLIENTES", "CREAR", "Crear clientes"},
            {"CLIENTES_EDITAR", "CLIENTES", "EDITAR", "Editar clientes"},
            {"COMISIONES_VER", "COMISIONES", "VER", "Ver comisiones"},
            {"COMISIONES_EDITAR", "COMISIONES", "EDITAR", "Marcar comisiones como pagadas"},
            {"REPORTES_VER", "REPORTES", "VER", "Ver reportes y resúmenes"},
            {"REPORTES_EXPORTAR", "REPORTES", "EXPORTAR", "Exportar reportes"},
            {"AUDITORIA_VER", "AUDITORIA", "VER", "Ver bitácora de auditoría"}
    };

    private static final String[][] ROLES_SEED = {
            {"ADMIN", "Administrador del sistema", "TODOS"},
            {"SUPERVISOR", "Supervisor de agentes de venta",
                    "DASHBOARD_VER,VENTAS_VER,VENTAS_ANULAR,CLIENTES_VER,COMISIONES_VER,COMISIONES_EDITAR,REPORTES_VER,REPORTES_EXPORTAR"},
            {"BACKOFFICE", "Validación y control de ventas",
                    "VENTAS_VER,VENTAS_APROBAR,VENTAS_RECHAZAR,VENTAS_ANULAR,CLIENTES_VER"},
            {"AGENTE", "Agente de ventas",
                    "VENTAS_VER,VENTAS_CREAR,CLIENTES_VER,CLIENTES_CREAR,CLIENTES_EDITAR"}
    };

    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;
    private final PlanRepository planRepository;
    private final DistritoRepository distritoRepository;
    private final ClienteRepository clienteRepository;
    private final VentaHistorialRepository historialRepository;
    private final ComisionRepository comisionRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        inicializarPermisosYRoles();
        if (planRepository.count() == 0) {
            crearPlanesSeed();
        }
        if (distritoRepository.count() == 0) {
            crearDistritosSeed();
        }
        crearUsuariosSeed();
    }

    private void inicializarPermisosYRoles() {
        if (permisoRepository.count() == 0) {
            List<Permiso> permisos = new ArrayList<>();
            for (String[] p : PERMISOS_SEED) {
                permisos.add(Permiso.builder().codigo(p[0]).modulo(p[1]).accion(p[2]).descripcion(p[3]).build());
            }
            permisoRepository.saveAll(permisos);
        }
        if (rolRepository.count() == 0) {
            for (String[] r : ROLES_SEED) {
                crearRol(r[0], r[1], r[2]);
            }
        }
    }

    private Rol crearRol(String nombre, String descripcion, String permisosText) {
        List<String> codigos;
        if ("TODOS".equals(permisosText)) {
            codigos = permisoRepository.findAll().stream().map(Permiso::getCodigo).toList();
        } else {
            codigos = List.of(permisosText.split(","));
        }
        Rol rol = Rol.builder()
                .nombre(nombre).descripcion(descripcion).activo(true)
                .permisos(new HashSet<>(permisoRepository.findByCodigoIn(codigos)))
                .build();
        return rolRepository.save(rol);
    }

    private void backfillRolId() {
        try {
            if (!columnaExiste("rol")) {
                return;
            }
            jdbcTemplate.update(
                    "UPDATE usuarios SET rol_id = (SELECT r.id FROM roles r WHERE r.nombre = usuarios.rol) WHERE rol_id IS NULL");
            jdbcTemplate.execute("ALTER TABLE usuarios DROP COLUMN rol");
        } catch (Exception ignored) {
        }
    }

    private boolean columnaExiste(String columna) {
        try {
            List<Map<String, Object>> filas = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'USUARIOS' AND COLUMN_NAME = ?",
                    columna.toUpperCase());
            if (!filas.isEmpty()) {
                return true;
            }
            filas = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'usuarios' AND COLUMN_NAME = ?",
                    columna);
            return !filas.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void crearUsuariosSeed() {
        Usuario supervisor1 = crearOActualizarUsuario("supervisor1", "Sup*123", "SUPERVISOR", null);
        Usuario back1 = crearOActualizarUsuario("back1", "Back*123", "BACKOFFICE", null);
        Usuario agente1 = crearOActualizarUsuario("agente1", "Agente*123", "AGENTE", supervisor1.getId());
        Usuario agente2 = crearOActualizarUsuario("agente2", "Agente*123", "AGENTE", supervisor1.getId());
        crearOActualizarUsuario("admin", "Admin*123", "ADMIN", null);

        if (ventaRepository.count() == 0) {
            crearVentasSeed(agente1, agente2, back1);
        }
    }

    private Usuario crearOActualizarUsuario(String username, String password, String rolNombre, Long supervisorId) {
        Long rolId = rolRepository.findByNombre(rolNombre)
                .map(Rol::getId)
                .orElse(null);
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        if (usuario == null) {
            usuario = Usuario.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .rolId(rolId)
                    .supervisorId(supervisorId)
                    .activo(true)
                    .build();
        } else {
            usuario.setPassword(passwordEncoder.encode(password));
            if (rolId != null) {
                usuario.setRolId(rolId);
            }
            if (supervisorId != null) {
                usuario.setSupervisorId(supervisorId);
            }
            usuario.setActivo(true);
        }
        return usuarioRepository.save(usuario);
    }

    private void crearPlanesSeed() {
        planRepository.saveAll(List.of(
                Plan.builder().codigo("FIJA_30").nombre("Plan Básico 30Mbps").tipo("FIJA")
                        .velocidadMbps(30).precioBase(new BigDecimal("39.90"))
                        .descripcion("Telefonía fija básica con internet 30Mbps").activo(true).build(),
                Plan.builder().codigo("FIJA_50").nombre("Plan Básico 50Mbps").tipo("FIJA")
                        .velocidadMbps(50).precioBase(new BigDecimal("49.90"))
                        .descripcion("Telefonía fija con internet 50Mbps").activo(true).build(),
                Plan.builder().codigo("HOGAR_100").nombre("Plan Hogar 100Mbps").tipo("HOGAR")
                        .velocidadMbps(100).precioBase(new BigDecimal("69.90"))
                        .descripcion("Internet hogar 100Mbps + llamadas").activo(true).build(),
                Plan.builder().codigo("HOGAR_200").nombre("Plan Hogar 200Mbps").tipo("HOGAR")
                        .velocidadMbps(200).precioBase(new BigDecimal("89.90"))
                        .descripcion("Internet hogar 200Mbps + llamadas ilimitadas").activo(true).build(),
                Plan.builder().codigo("HOGAR_300").nombre("Plan Triple 300Mbps").tipo("HOGAR")
                        .velocidadMbps(300).precioBase(new BigDecimal("129.90"))
                        .descripcion("Internet 300Mbps + TV + llamadas").activo(true).build(),
                Plan.builder().codigo("EMPR_500").nombre("Plan Empresarial 500Mbps").tipo("EMPRESARIAL")
                        .velocidadMbps(500).precioBase(new BigDecimal("199.90"))
                        .descripcion("Internet empresarial 500Mbps con soporte").activo(true).build()
        ));
    }

    private void crearDistritosSeed() {
        distritoRepository.saveAll(List.of(
                Distrito.builder().nombre("Lima").provincia("Lima").departamento("Lima").codigoUbigeo("150101").activo(true).build(),
                Distrito.builder().nombre("Miraflores").provincia("Lima").departamento("Lima").codigoUbigeo("150122").activo(true).build(),
                Distrito.builder().nombre("San Isidro").provincia("Lima").departamento("Lima").codigoUbigeo("150131").activo(true).build(),
                Distrito.builder().nombre("San Miguel").provincia("Lima").departamento("Lima").codigoUbigeo("150136").activo(true).build(),
                Distrito.builder().nombre("Pueblo Libre").provincia("Lima").departamento("Lima").codigoUbigeo("150121").activo(true).build(),
                Distrito.builder().nombre("La Victoria").provincia("Lima").departamento("Lima").codigoUbigeo("150115").activo(true).build(),
                Distrito.builder().nombre("San Borja").provincia("Lima").departamento("Lima").codigoUbigeo("150130").activo(true).build(),
                Distrito.builder().nombre("Los Olivos").provincia("Lima").departamento("Lima").codigoUbigeo("150117").activo(true).build(),
                Distrito.builder().nombre("Breña").provincia("Lima").departamento("Lima").codigoUbigeo("150105").activo(true).build(),
                Distrito.builder().nombre("Callao").provincia("Callao").departamento("Callao").codigoUbigeo("070101").activo(true).build(),
                Distrito.builder().nombre("Arequipa").provincia("Arequipa").departamento("Arequipa").codigoUbigeo("040101").activo(true).build(),
                Distrito.builder().nombre("Trujillo").provincia("Trujillo").departamento("La Libertad").codigoUbigeo("130101").activo(true).build()
        ));
    }

    private void crearVentasSeed(Usuario agente1, Usuario agente2, Usuario backoffice) {
        LocalDateTime base = LocalDateTime.now().minusDays(5);

        Plan hogar200 = planRepository.findByCodigo("HOGAR_200").orElse(null);
        Plan hogar100 = planRepository.findByCodigo("HOGAR_100").orElse(null);
        Plan hogar300 = planRepository.findByCodigo("HOGAR_300").orElse(null);

        Cliente c1 = crearCliente("12345678", "Juan Pérez", "912345678", "Av. Siempre Viva 123, Lima", "Miraflores");
        Cliente c2 = crearCliente("87654321", "María López", "987654321", "Jr. Las Flores 456, Miraflores", "Miraflores");
        Cliente c3 = crearCliente("44556677", "Carlos Ruiz", "955667788", "Av. Universitaria 789, San Miguel", "San Miguel");
        Cliente c4 = crearCliente("20304050", "Ana Torres", "933445566", "Calle Los Pinos 321, Breña", "Breña");
        Cliente c5 = crearCliente("10111213", "Pedro Díaz", "911223344", "Av. Aviación 654, San Borja", "San Borja");
        Cliente c6 = crearCliente("12131415", "Lucía Fernández", "977889900", "Jr. Huallaga 987, Pueblo Libre", "Pueblo Libre");
        Cliente c7 = crearCliente("20212223", "José Morales", "922334455", "Av. Tupac Amaru 147, Los Olivos", "Los Olivos");
        Cliente c8 = crearCliente("10987654", "Carla Vargas", "999888777", "Calle Berlin 369, San Isidro", "San Isidro");

        Venta v1 = Venta.builder()
                .agenteId(agente1.getId())
                .dniCliente(c1.getDni()).nombreCliente(c1.getNombreCliente())
                .telefonoCliente(c1.getTelefono()).direccionCliente(c1.getDireccion())
                .planActual("Plan Básico 50Mbps").planNuevo("Plan Premium 200Mbps")
                .codigoLlamada("CALL-001").producto("FIJA_HOGAR")
                .monto(new BigDecimal("89.90")).estado(EstadoVenta.PENDIENTE)
                .fechaRegistro(base).clienteId(c1.getId())
                .planId(hogar200 != null ? hogar200.getId() : null)
                .build();

        Venta v2 = Venta.builder()
                .agenteId(agente1.getId())
                .dniCliente(c2.getDni()).nombreCliente(c2.getNombreCliente())
                .telefonoCliente(c2.getTelefono()).direccionCliente(c2.getDireccion())
                .planActual("Plan Fibra 100Mbps").planNuevo("Plan Full 500Mbps + TV")
                .codigoLlamada("CALL-002").producto("FIJA_HOGAR")
                .monto(new BigDecimal("159.90")).estado(EstadoVenta.APROBADA)
                .fechaRegistro(base.plusHours(2)).fechaValidacion(base.plusDays(1).plusHours(1))
                .clienteId(c2.getId())
                .planId(hogar300 != null ? hogar300.getId() : null)
                .build();

        Venta v3 = Venta.builder()
                .agenteId(agente1.getId())
                .dniCliente(c3.getDni()).nombreCliente(c3.getNombreCliente())
                .telefonoCliente(c3.getTelefono()).direccionCliente(c3.getDireccion())
                .planActual(null).planNuevo("Plan Nuevo Hogar 100Mbps")
                .codigoLlamada("CALL-003").producto("FIJA_HOGAR")
                .monto(new BigDecimal("69.90")).estado(EstadoVenta.RECHAZADA)
                .motivoRechazo("Cliente no cuenta con línea telefónica disponible en la zona")
                .fechaRegistro(base.plusDays(1)).fechaValidacion(base.plusDays(2))
                .clienteId(c3.getId())
                .planId(hogar100 != null ? hogar100.getId() : null)
                .build();

        Venta v4 = Venta.builder()
                .agenteId(agente2.getId())
                .dniCliente(c4.getDni()).nombreCliente(c4.getNombreCliente())
                .telefonoCliente(c4.getTelefono()).direccionCliente(c4.getDireccion())
                .planActual("Plan Económico 30Mbps").planNuevo("Plan Doble 100Mbps + Llamadas ilimitadas")
                .codigoLlamada("CALL-004").producto("FIJA_HOGAR")
                .monto(new BigDecimal("79.90")).estado(EstadoVenta.PENDIENTE)
                .fechaRegistro(base.plusDays(2)).clienteId(c4.getId())
                .planId(hogar100 != null ? hogar100.getId() : null)
                .build();

        Venta v5 = Venta.builder()
                .agenteId(agente2.getId())
                .dniCliente(c5.getDni()).nombreCliente(c5.getNombreCliente())
                .telefonoCliente(c5.getTelefono()).direccionCliente(c5.getDireccion())
                .planActual("Plan 200Mbps").planNuevo("Plan Premium 500Mbps + TV HD")
                .codigoLlamada("CALL-005").producto("FIJA_HOGAR")
                .monto(new BigDecimal("199.90")).estado(EstadoVenta.APROBADA)
                .fechaRegistro(base.plusDays(2).plusHours(5)).fechaValidacion(base.plusDays(3))
                .clienteId(c5.getId())
                .planId(hogar300 != null ? hogar300.getId() : null)
                .build();

        Venta v6 = Venta.builder()
                .agenteId(agente1.getId())
                .dniCliente(c6.getDni()).nombreCliente(c6.getNombreCliente())
                .telefonoCliente(c6.getTelefono()).direccionCliente(c6.getDireccion())
                .planActual("Plan 50Mbps").planNuevo("Plan Triple 300Mbps + TV + Llamadas")
                .codigoLlamada("CALL-006").producto("FIJA_HOGAR")
                .monto(new BigDecimal("129.90")).estado(EstadoVenta.RECHAZADA)
                .motivoRechazo("Documentación incompleta del cliente")
                .fechaRegistro(base.plusDays(3)).fechaValidacion(base.plusDays(3).plusHours(3))
                .clienteId(c6.getId())
                .planId(hogar300 != null ? hogar300.getId() : null)
                .build();

        Venta v7 = Venta.builder()
                .agenteId(agente2.getId())
                .dniCliente(c7.getDni()).nombreCliente(c7.getNombreCliente())
                .telefonoCliente(c7.getTelefono()).direccionCliente(c7.getDireccion())
                .planActual(null).planNuevo("Plan Hogar 150Mbps + Fijo")
                .codigoLlamada("CALL-007").producto("FIJA_HOGAR")
                .monto(new BigDecimal("99.90")).estado(EstadoVenta.PENDIENTE)
                .fechaRegistro(LocalDateTime.now().minusHours(3)).clienteId(c7.getId())
                .planId(hogar200 != null ? hogar200.getId() : null)
                .build();

        Venta v8 = Venta.builder()
                .agenteId(agente1.getId())
                .dniCliente(c8.getDni()).nombreCliente(c8.getNombreCliente())
                .telefonoCliente(c8.getTelefono()).direccionCliente(c8.getDireccion())
                .planActual("Plan 100Mbps").planNuevo("Plan Empresarial 1Gbps")
                .codigoLlamada("CALL-008").producto("FIJA_HOGAR")
                .monto(new BigDecimal("299.90")).estado(EstadoVenta.APROBADA)
                .fechaRegistro(LocalDateTime.now().minusDays(1)).fechaValidacion(LocalDateTime.now().minusHours(12))
                .clienteId(c8.getId())
                .planId(hogar300 != null ? hogar300.getId() : null)
                .build();

        ventaRepository.saveAll(List.of(v1, v2, v3, v4, v5, v6, v7, v8));

        registrarHistorial(v1.getId(), null, "PENDIENTE", agente1, null);
        registrarHistorial(v2.getId(), null, "PENDIENTE", agente1, null);
        registrarHistorial(v2.getId(), "PENDIENTE", "APROBADA", backoffice, null);
        registrarHistorial(v3.getId(), null, "PENDIENTE", agente1, null);
        registrarHistorial(v3.getId(), "PENDIENTE", "RECHAZADA", backoffice, v3.getMotivoRechazo());
        registrarHistorial(v4.getId(), null, "PENDIENTE", agente2, null);
        registrarHistorial(v5.getId(), null, "PENDIENTE", agente2, null);
        registrarHistorial(v5.getId(), "PENDIENTE", "APROBADA", backoffice, null);
        registrarHistorial(v6.getId(), null, "PENDIENTE", agente1, null);
        registrarHistorial(v6.getId(), "PENDIENTE", "RECHAZADA", backoffice, v6.getMotivoRechazo());
        registrarHistorial(v7.getId(), null, "PENDIENTE", agente2, null);
        registrarHistorial(v8.getId(), null, "PENDIENTE", agente1, null);
        registrarHistorial(v8.getId(), "PENDIENTE", "APROBADA", backoffice, null);

        crearComision(v2, backoffice);
        crearComision(v5, backoffice);
        crearComision(v8, backoffice);
    }

    private Cliente crearCliente(String dni, String nombre, String telefono, String direccion, String distritoNombre) {
        return clienteRepository.findByDni(dni).orElseGet(() -> {
            Long distritoId = distritoRepository.findByActivoTrueOrderByNombreAsc().stream()
                    .filter(d -> d.getNombre().equalsIgnoreCase(distritoNombre))
                    .findFirst().map(Distrito::getId).orElse(null);
            return clienteRepository.save(Cliente.builder()
                    .dni(dni).nombreCliente(nombre).telefono(telefono).direccion(direccion)
                    .distritoId(distritoId).activo(true).build());
        });
    }

    private void registrarHistorial(Long ventaId, String estadoAnterior, String estadoNuevo,
                                    Usuario usuario, String motivo) {
        historialRepository.save(VentaHistorial.builder()
                .ventaId(ventaId).estadoAnterior(estadoAnterior).estadoNuevo(estadoNuevo)
                .usuarioId(usuario.getId()).motivo(motivo).build());
    }

    private void crearComision(Venta venta, Usuario backoffice) {
        if (comisionRepository.findByVentaId(venta.getId()).isPresent()) {
            return;
        }
        BigDecimal porcentaje = new BigDecimal("5.00");
        BigDecimal montoComision = venta.getMonto().multiply(porcentaje)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        comisionRepository.save(Comision.builder()
                .ventaId(venta.getId()).agenteId(venta.getAgenteId())
                .montoBase(venta.getMonto()).porcentaje(porcentaje)
                .montoComision(montoComision).estado("PENDIENTE").build());
        backoffice.getUsername();
    }
}
