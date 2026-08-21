DROP TABLE IF EXISTS auditoria;
DROP TABLE IF EXISTS comisiones;
DROP TABLE IF EXISTS venta_historial;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS distritos;
DROP TABLE IF EXISTS planes;
DROP TABLE IF EXISTS ventas;
DROP TABLE IF EXISTS usuarios;

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    supervisor_id BIGINT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_supervisor FOREIGN KEY (supervisor_id) REFERENCES usuarios(id)
);

CREATE TABLE distritos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    provincia VARCHAR(80) NOT NULL,
    departamento VARCHAR(80) NOT NULL,
    codigo_ubigeo VARCHAR(10),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    dni VARCHAR(11) NOT NULL UNIQUE,
    nombre_cliente VARCHAR(255) NOT NULL,
    telefono VARCHAR(9) NOT NULL,
    direccion VARCHAR(500) NOT NULL,
    distrito_id BIGINT,
    email VARCHAR(100),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clientes_distrito FOREIGN KEY (distrito_id) REFERENCES distritos(id)
);

CREATE TABLE planes (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    velocidad_mbps INT,
    precio_base DECIMAL(10,2) NOT NULL DEFAULT 0,
    descripcion VARCHAR(300),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ventas (
    id BIGSERIAL PRIMARY KEY,
    agente_id BIGINT NOT NULL,
    dni_cliente VARCHAR(11) NOT NULL,
    nombre_cliente VARCHAR(255) NOT NULL,
    telefono_cliente VARCHAR(9) NOT NULL,
    direccion_cliente VARCHAR(500) NOT NULL,
    plan_actual VARCHAR(200),
    plan_nuevo VARCHAR(200) NOT NULL,
    codigo_llamada VARCHAR(50) NOT NULL UNIQUE,
    producto VARCHAR(30) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    motivo_rechazo VARCHAR(500),
    fecha_registro TIMESTAMP,
    fecha_validacion TIMESTAMP,
    cliente_id BIGINT,
    plan_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ventas_agente FOREIGN KEY (agente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_ventas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_ventas_plan FOREIGN KEY (plan_id) REFERENCES planes(id)
);

CREATE TABLE venta_historial (
    id BIGSERIAL PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20) NOT NULL,
    usuario_id BIGINT,
    motivo VARCHAR(500),
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT fk_hist_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE comisiones (
    id BIGSERIAL PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    agente_id BIGINT NOT NULL,
    monto_base DECIMAL(10,2) NOT NULL,
    porcentaje DECIMAL(5,2) NOT NULL,
    monto_comision DECIMAL(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_pago TIMESTAMP,
    CONSTRAINT fk_com_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT fk_com_agente FOREIGN KEY (agente_id) REFERENCES usuarios(id)
);

CREATE TABLE auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    usuario_username VARCHAR(50),
    accion VARCHAR(50) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    entidad_id BIGINT,
    detalle VARCHAR(500),
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aud_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_ventas_agente ON ventas(agente_id);
CREATE INDEX idx_ventas_estado ON ventas(estado);
CREATE INDEX idx_ventas_fecha ON ventas(fecha_registro);
CREATE INDEX idx_usuarios_supervisor ON usuarios(supervisor_id);
CREATE INDEX idx_clientes_dni ON clientes(dni);
CREATE INDEX idx_planes_activo ON planes(activo);
CREATE INDEX idx_historial_venta ON venta_historial(venta_id);
CREATE INDEX idx_comisiones_agente ON comisiones(agente_id);
CREATE INDEX idx_auditoria_fecha ON auditoria(fecha);
