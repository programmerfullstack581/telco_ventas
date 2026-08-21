-- MySQL 8.x Schema para Ventas Telco Fija Hogar
-- Incluye tablas de catalogo: roles, estados, productos, equipo, planes, distritos, clientes
-- Mas tablas de control: venta_historial, comisiones, auditoria

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS auditoria;
DROP TABLE IF EXISTS comisiones;
DROP TABLE IF EXISTS venta_historial;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS distritos;
DROP TABLE IF EXISTS planes;
DROP TABLE IF EXISTS ventas;
DROP TABLE IF EXISTS equipo;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS estados;
DROP TABLE IF EXISTS roles;

SET FOREIGN_KEY_CHECKS = 1;

-- Catalogo de roles
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(200) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Catalogo de estados de venta
CREATE TABLE estados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(200) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Catalogo de productos
CREATE TABLE productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(200) NULL,
    precio DECIMAL(10,2) NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Catalogo de distritos / geografia
CREATE TABLE distritos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    provincia VARCHAR(80) NOT NULL,
    departamento VARCHAR(80) NOT NULL,
    codigo_ubigeo VARCHAR(10) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Catalogo de clientes
CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dni VARCHAR(11) NOT NULL UNIQUE,
    nombre_cliente VARCHAR(255) NOT NULL,
    telefono VARCHAR(9) NOT NULL,
    direccion VARCHAR(500) NOT NULL,
    distrito_id BIGINT NULL,
    email VARCHAR(100) NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_clientes_distrito FOREIGN KEY (distrito_id) REFERENCES distritos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Catalogo de planes de internet / telefonia fija
CREATE TABLE planes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    velocidad_mbps INT NULL,
    precio_base DECIMAL(10,2) NOT NULL DEFAULT 0,
    descripcion VARCHAR(300) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Usuarios (rol referenciado al catalogo roles)
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    supervisor_id BIGINT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_supervisor FOREIGN KEY (supervisor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol) REFERENCES roles(nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ventas (estado y producto referenciados a sus catalogos)
CREATE TABLE ventas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agente_id BIGINT NOT NULL,
    dni_cliente VARCHAR(11) NOT NULL,
    nombre_cliente VARCHAR(255) NOT NULL,
    telefono_cliente VARCHAR(9) NOT NULL,
    direccion_cliente VARCHAR(500) NOT NULL,
    plan_actual VARCHAR(200) NULL,
    plan_nuevo VARCHAR(200) NOT NULL,
    codigo_llamada VARCHAR(50) NOT NULL UNIQUE,
    producto VARCHAR(30) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    motivo_rechazo VARCHAR(500) NULL,
    fecha_registro TIMESTAMP NULL,
    fecha_validacion TIMESTAMP NULL,
    cliente_id BIGINT NULL,
    plan_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ventas_agente FOREIGN KEY (agente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_ventas_estado FOREIGN KEY (estado) REFERENCES estados(nombre),
    CONSTRAINT fk_ventas_producto FOREIGN KEY (producto) REFERENCES productos(nombre),
    CONSTRAINT fk_ventas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_ventas_plan FOREIGN KEY (plan_id) REFERENCES planes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Historial de cambios de estado de cada venta
CREATE TABLE venta_historial (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    estado_anterior VARCHAR(20) NULL,
    estado_nuevo VARCHAR(20) NOT NULL,
    usuario_id BIGINT NULL,
    motivo VARCHAR(500) NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT fk_hist_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comisiones generadas por ventas aprobadas
CREATE TABLE comisiones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    agente_id BIGINT NOT NULL,
    monto_base DECIMAL(10,2) NOT NULL,
    porcentaje DECIMAL(5,2) NOT NULL,
    monto_comision DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_pago TIMESTAMP NULL,
    CONSTRAINT fk_com_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT fk_com_agente FOREIGN KEY (agente_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bitacora de acciones del sistema
CREATE TABLE auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NULL,
    usuario_username VARCHAR(50) NULL,
    accion VARCHAR(50) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    entidad_id BIGINT NULL,
    detalle VARCHAR(500) NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aud_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Equipo: relacion supervisor <-> agentes a su mando
CREATE TABLE equipo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supervisor_id BIGINT NOT NULL,
    agente_id BIGINT NOT NULL,
    fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_equipo_supervisor_agente (supervisor_id, agente_id),
    CONSTRAINT fk_equipo_supervisor FOREIGN KEY (supervisor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_equipo_agente FOREIGN KEY (agente_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ventas_agente ON ventas(agente_id);
CREATE INDEX idx_ventas_estado ON ventas(estado);
CREATE INDEX idx_ventas_fecha ON ventas(fecha_registro);
CREATE INDEX idx_usuarios_supervisor ON usuarios(supervisor_id);
CREATE INDEX idx_clientes_dni ON clientes(dni);
CREATE INDEX idx_planes_activo ON planes(activo);
CREATE INDEX idx_historial_venta ON venta_historial(venta_id);
CREATE INDEX idx_comisiones_agente ON comisiones(agente_id);
CREATE INDEX idx_auditoria_fecha ON auditoria(fecha);
