-- Datos semilla de CATALOGOS para el perfil MySQL.
-- Los usuarios y ventas demo los crea DataInitializer (BCrypt) al arrancar.

-- Roles
INSERT INTO roles (nombre, descripcion) VALUES
('ADMIN', 'Administrador general del sistema'),
('SUPERVISOR', 'Supervisor de equipo de ventas'),
('BACKOFFICE', 'Analista que valida (aprueba/rechaza) ventas'),
('AGENTE', 'Agente de ventas que registra las ventas');

-- Estados de venta
INSERT INTO estados (nombre, descripcion) VALUES
('PENDIENTE', 'Venta registrada, esperando validacion del backoffice'),
('APROBADA', 'Venta validada y aprobada por el backoffice'),
('RECHAZADA', 'Venta rechazada por el backoffice con motivo de rechazo');

-- Productos
INSERT INTO productos (nombre, descripcion, precio) VALUES
('FIJA', 'Telefonia fija hogar basica', 89.90),
('FIJA_HOGAR', 'Plan hogar integral de telefonia fija', 149.90);

-- Distritos / geografia
INSERT INTO distritos (nombre, provincia, departamento, codigo_ubigeo) VALUES
('Lima', 'Lima', 'Lima', '150101'),
('Miraflores', 'Lima', 'Lima', '150122'),
('San Isidro', 'Lima', 'Lima', '150131'),
('San Miguel', 'Lima', 'Lima', '150136'),
('Pueblo Libre', 'Lima', 'Lima', '150121'),
('La Victoria', 'Lima', 'Lima', '150115'),
('San Borja', 'Lima', 'Lima', '150130'),
('Los Olivos', 'Lima', 'Lima', '150117'),
('Brena', 'Lima', 'Lima', '150105'),
('Callao', 'Callao', 'Callao', '070101'),
('Arequipa', 'Arequipa', 'Arequipa', '040101'),
('Trujillo', 'Trujillo', 'La Libertad', '130101');

-- Planes de internet / telefonia fija
INSERT INTO planes (codigo, nombre, tipo, velocidad_mbps, precio_base, descripcion) VALUES
('FIJA_30', 'Plan Basico 30Mbps', 'FIJA', 30, 39.90, 'Telefonia fija basica con internet 30Mbps'),
('FIJA_50', 'Plan Basico 50Mbps', 'FIJA', 50, 49.90, 'Telefonia fija con internet 50Mbps'),
('HOGAR_100', 'Plan Hogar 100Mbps', 'HOGAR', 100, 69.90, 'Internet hogar 100Mbps + llamadas'),
('HOGAR_200', 'Plan Hogar 200Mbps', 'HOGAR', 200, 89.90, 'Internet hogar 200Mbps + llamadas ilimitadas'),
('HOGAR_300', 'Plan Triple 300Mbps', 'HOGAR', 300, 129.90, 'Internet 300Mbps + TV + llamadas'),
('EMPR_500', 'Plan Empresarial 500Mbps', 'EMPRESARIAL', 500, 199.90, 'Internet empresarial 500Mbps con soporte');

SELECT 1;
