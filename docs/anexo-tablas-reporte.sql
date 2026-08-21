-- =====================================================================
-- VENTAS TELCO FIJA HOGAR - ANEXO DE TABLAS DE CATALOGO Y REPORTE
-- Anexa las tablas: roles, estados, productos, equipo (supervisor->agentes)
-- Mantiene intactas las tablas existentes (usuarios, ventas) para que el
-- backend y el frontend sigan funcionando igual.
-- Ejecutar UNA sola vez en phpMyAdmin (pestana SQL).
-- =====================================================================

USE telco_ventas;

-- ---------------------------------------------------------------------
-- 1) TABLA ROLES (catalogo de roles de usuario)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(200) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (nombre, descripcion) VALUES
('ADMIN', 'Administrador general del sistema'),
('SUPERVISOR', 'Supervisor de equipo de ventas'),
('BACKOFFICE', 'Analista que valida (aprueba/rechaza) ventas'),
('AGENTE', 'Agente de ventas que registra las ventas')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- ---------------------------------------------------------------------
-- 2) TABLA ESTADOS (estados por los que pasa una venta)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS estados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(200) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO estados (nombre, descripcion) VALUES
('PENDIENTE', 'Venta registrada, esperando validacion del backoffice'),
('APROBADA', 'Venta validada y aprobada por el backoffice'),
('RECHAZADA', 'Venta rechazada por el backoffice con motivo de rechazo')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- ---------------------------------------------------------------------
-- 3) TABLA PRODUCTOS (catalogo de productos ofrecidos)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(200) NULL,
    precio DECIMAL(10,2) NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO productos (nombre, descripcion, precio) VALUES
('FIJA', 'Telefonia fija hogar basica', 89.90),
('FIJA_HOGAR', 'Plan hogar integral de telefonia fija', 149.90)
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion), precio = VALUES(precio);

-- ---------------------------------------------------------------------
-- 4) TABLA EQUIPO (relacion supervisor <-> agentes bajo su mando)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS equipo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supervisor_id BIGINT NOT NULL,
    agente_id BIGINT NOT NULL,
    fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_equipo_supervisor_agente (supervisor_id, agente_id),
    CONSTRAINT fk_equipo_supervisor FOREIGN KEY (supervisor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_equipo_agente FOREIGN KEY (agente_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO equipo (supervisor_id, agente_id) VALUES
(2, 4),
(2, 5);

-- ---------------------------------------------------------------------
-- 5) RELACIONES (FK) sobre las tablas existentes SIN modificar columnas
--    Usa el nombre como clave natural para no romper el backend.
--    Si la venta o el rol no existe en el catalogo, la FK lo impide.
-- ---------------------------------------------------------------------
ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol) REFERENCES roles(nombre);

ALTER TABLE ventas
    ADD CONSTRAINT fk_ventas_estado FOREIGN KEY (estado) REFERENCES estados(nombre),
    ADD CONSTRAINT fk_ventas_producto FOREIGN KEY (producto) REFERENCES productos(nombre);

-- =====================================================================
-- CONSULTAS DE REPORTE (opcionales, para validar el modelo completo)
-- =====================================================================
-- Resumen de ventas por estado con catalogo de estados
-- SELECT e.nombre AS estado, COUNT(v.id) AS cantidad, COALESCE(SUM(v.monto),0) AS monto_total
-- FROM estados e LEFT JOIN ventas v ON v.estado = e.nombre
-- GROUP BY e.id, e.nombre;

-- Ventas por agente con su supervisor (via equipo)
-- SELECT u.username AS agente, s.username AS supervisor, COUNT(v.id) AS ventas, COALESCE(SUM(v.monto),0) AS monto
-- FROM usuarios u
-- LEFT JOIN equipo eq ON eq.agente_id = u.id
-- LEFT JOIN usuarios s ON s.id = eq.supervisor_id
-- LEFT JOIN ventas v ON v.agente_id = u.id
-- WHERE u.rol = 'AGENTE'
-- GROUP BY u.id, s.id;
