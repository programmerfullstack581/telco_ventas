# 📞 Ventas Telco Fija Hogar

Sistema de gestión de ventas **Telco Fija Hogar**: el **Agente registra** → el **Backoffice aprueba/rechaza** → el **Supervisor reportea** su equipo.

## 🧩 Stack

| Capa | Tecnología |
|------|------------|
| **Backend** | Java 21 + Spring Boot 3.2.5 + Spring Security (JWT) + Spring Data JPA |
| **Base de datos** | H2 (default) / **MySQL 8.x (recomendado)** / PostgreSQL |
| **Frontend** | Vite 5 + React 18 + React Router + Axios (SPA) |
| **Documentación API** | OpenAPI / Swagger UI |

## 🚀 Ejecución rápida

### Requisitos
- JDK 21 LTS *(el script `setup-and-run.ps1` lo detecta o descarga un JDK 21 portable automáticamente)*
- Node.js 20 LTS
- MySQL 8.x *(el usuario de Laragon usa `root` sin contraseña)*

### 1) Backend

Abre una terminal y ejecuta:

```powershell
cd E:\telco_ventas\backend
call "E:\telco_ventas\backend\.tools\apache-maven-3.9.9\bin\mvn.cmd" -q spring-boot:run
```

> Si usas **cmd.exe** en vez de PowerShell:
> ```powershell
> powershell -ExecutionPolicy Bypass -Command "cd E:\telco_ventas\backend; .\setup-and-run.ps1 mysql"
> ```

**¿Qué hace `setup-and-run.ps1`?**
1. Detecta JDK 21 (o usa el portable en `backend/.tools/jdk-21*`).
2. Si no hay Maven global, descarga Maven 3.9.9 portable en `backend/.tools/`.
3. Activa el perfil Spring elegido (`mysql` / `postgres` / `default`).
4. Ejecuta `spring-boot:run`.

✅ Cuando veas `Started VentasTelcoApplication`, el backend está arriba en **http://localhost:8081** (perfil MySQL) o **http://localhost:8080** (H2 default).

### 2) Frontend

Abre una segunda terminal y ejecuta:

```powershell
cd E:\telco_ventas\frontend
npm install
npm run dev
```

✅ Abre **http://localhost:5173** e inicia sesión. Vite redirige `/api` al backend (`vite.config.js` apunta a `http://localhost:8081`).

### Otros perfiles

```powershell
# H2 en memoria (sin MySQL instalado)
.\setup-and-run.ps1 default

# PostgreSQL
.\setup-and-run.ps1 postgres
```

## 🔐 Configuración MySQL

Las credenciales se definen en `backend/src/main/resources/application-mysql.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/telco_ventas?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=
```

> **Importante:** en **Laragon** el usuario `root` no tiene contraseña (dejar `spring.datasource.password=` vacío). Si tu MySQL usa otra contraseña, cámbiala ahí.

## 👥 Usuarios demo

| Usuario     | Contraseña | Rol          |
|-------------|------------|--------------|
| admin       | Admin*123  | ADMIN        |
| supervisor1 | Sup*123    | SUPERVISOR   |
| back1       | Back*123   | BACKOFFICE   |
| agente1     | Agente*123 | AGENTE       |
| agente2     | Agente*123 | AGENTE       |

*Las contraseñas se guardan encriptadas con **BCrypt**.* Si reinsertas los usuarios manualmente, usa los hashes del script `docs/anexo-tablas-reporte.sql` (los que vienen en este README no son válidos porque se generan al arrancar con `DataInitializer`).

## 🧭 URLs

| URL | Descripción |
|-----|-------------|
| http://localhost:5173 | SPA (Vite) |
| http://localhost:8081/api/v1 | Base API REST (perfil MySQL) |
| http://localhost:8081/swagger-ui.html | Swagger UI / OpenAPI |
| http://localhost:8081/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/h2-console | Consola H2 (solo perfil default) |

## 🗄️ Base de datos (MySQL) — Modelo de datos

Esquema con **6 tablas** y sus relaciones. Creación automática al arrancar con `schema-mysql.sql` + catálogos en `data-mysql.sql`. Para anexar/recrear manualmente en **phpMyAdmin**: `docs/anexo-tablas-reporte.sql`.

```
roles (id, nombre, descripcion)
   │ 1:N (usuarios.rol -> roles.nombre)
estados (id, nombre, descripcion)
   │ 1:N (ventas.estado -> estados.nombre)
productos (id, nombre, descripcion, precio, activo)
   │ 1:N (ventas.producto -> productos.nombre)
usuarios (id, username, password_hash, rol, supervisor_id, activo)
   ├── supervisor_id -> usuarios.id  (auto-referencia: jefe del usuario)
ventas (id, agente_id, dni_cliente, nombre_cliente, telefono_cliente,
        direccion_cliente, plan_actual, plan_nuevo, codigo_llamada,
        producto, monto, estado, motivo_rechazo, fecha_registro,
        fecha_validacion)
   └── agente_id -> usuarios.id
equipo (id, supervisor_id, agente_id, fecha_asignacion, activo)
   ├── supervisor_id -> usuarios.id
   └── agente_id -> usuarios.id
```

**Tablas nuevas (catálogo y reporte):**

| Tabla | Uso |
|-------|-----|
| `roles` | Catálogo de roles: ADMIN, SUPERVISOR, BACKOFFICE, AGENTE |
| `estados` | Catálogo de estados de venta: PENDIENTE, APROBADA, RECHAZADA |
| `productos` | Catálogo de productos: FIJA, FIJA_HOGAR (con precio) |
| `equipo` | Asignación de agentes a su supervisor (equipo a mando) |

### Consultas de reporte

```sql
-- Resumen de ventas por estado
SELECT e.nombre AS estado, COUNT(v.id) AS cantidad, COALESCE(SUM(v.monto),0) AS monto_total
FROM estados e LEFT JOIN ventas v ON v.estado = e.nombre
GROUP BY e.id, e.nombre;

-- Ventas por agente con su supervisor (via equipo)
SELECT u.username AS agente, s.username AS supervisor,
       COUNT(v.id) AS ventas, COALESCE(SUM(v.monto),0) AS monto
FROM usuarios u
LEFT JOIN equipo eq ON eq.agente_id = u.id
LEFT JOIN usuarios s ON s.id = eq.supervisor_id
LEFT JOIN ventas v ON v.agente_id = u.id
WHERE u.rol = 'AGENTE'
GROUP BY u.id, s.id;

-- Detalle completo de ventas
SELECT v.id, u.username AS agente, v.nombre_cliente, p.nombre AS producto,
       v.monto, e.nombre AS estado, v.motivo_rechazo,
       v.fecha_registro, v.fecha_validacion
FROM ventas v
JOIN usuarios u ON u.id = v.agente_id
JOIN estados e ON e.nombre = v.estado
JOIN productos p ON p.nombre = v.producto
ORDER BY v.fecha_registro DESC;
```

## 🔌 API REST (prefijo `/api/v1`)

### Auth
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/auth/login` | Público | Devuelve JWT con rol y userId |

### Ventas – Agente
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | `/ventas` | AGENTE | Crea venta en PENDIENTE (snapshot cliente) |
| GET | `/ventas/mis-ventas` | AGENTE | Propias ventas (filtros: `estado`, `desde`, `hasta` + paginación) |

### Ventas – Backoffice
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| GET | `/ventas/pendientes` | BACKOFFICE / ADMIN | Lista ventas PENDIENTES |
| POST | `/ventas/{id}/aprobar` | BACKOFFICE / ADMIN | Cambia a APROBADA + `fecha_validacion` |
| POST | `/ventas/{id}/rechazar` | BACKOFFICE / ADMIN | Cambia a RECHAZADA + `motivo_rechazo` (obligatorio) |

### Reportes – Supervisor
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| GET | `/ventas/equipo` | SUPERVISOR / ADMIN | Ventas de agentes bajo su `supervisor_id` |
| GET | `/ventas/reportes/resumen` | SUPERVISOR / ADMIN | Totales por estado, monto aprobado, serie `ventas_por_dia` |

> CORS habilitado para `http://localhost:5173` (configurable en `cors.allowed-origins`).

## 🧪 Colección Postman

Importa **`docs/Ventas-Telco.postman_collection.json`**. La carpeta `0. Auth` guarda el token automáticamente en variables de colección.

## ☁️ Despliegue en la nube (Vercel + Railway)

Arquitectura: **frontend (React/Vite) en Vercel** + **backend (Spring Boot) en Railway** + **MySQL**.
El frontend llama a `/api/v1/...` (ruta relativa); Vercel hace proxy de `/api/*` hacia el backend, así no hay problemas de CORS.

### 1) Backend en Railway

1. Entra a [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub repo** → selecciona `telco_ventas`.
2. En el servicio creado → **Settings**:
   - **Root Directory**: `backend`
   - Railway detecta el `Dockerfile` automáticamente.
3. Agrega MySQL: botón **+ New** → **Database** → **MySQL** (se crea con variables `MYSQLHOST`, `MYSQLPORT`, etc.).
4. En el servicio del backend → **Variables**, agrega:

   | Variable | Valor |
   |----------|-------|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima` |
   | `DB_USER` | `${{MySQL.MYSQLUSER}}` |
   | `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
   | `JWT_SECRET` | una cadena larga y aleatoria (ej. `openssl rand -base64 64`) |
   | `CORS_ORIGINS` | `https://tu-app.vercel.app` (complétalo cuando exista) |

5. **Settings → Networking → Generate Domain**: obtendrás algo como `https://ventas-telco-production.up.railway.app`.
6. Verifica: `GET https://<tu-backend>.up.railway.app/swagger-ui.html`.

> 💡 Alternativa: usa tu MySQL de Aiven existente cambiando `DB_URL/DB_USER/DB_PASSWORD` por las credenciales de Aiven (`sslMode=REQUIRED`). Así conservas los datos actuales.

### 2) Frontend en Vercel

1. Entra a [vercel.com](https://vercel.com) → **Add New Project** → importa `telco_ventas`.
2. Configura:
   - **Root Directory**: `frontend`
   - Framework Preset: **Vite** (autodetectado)
3. Antes de desplegar, edita `frontend/vercel.json` y reemplaza `REEMPLAZA-ESTA-URL.up.railway.app` por el dominio real del backend del paso anterior.
4. **Deploy**. La app quedará en `https://tu-app.vercel.app`.
5. Vuelve a Railway y completa `CORS_ORIGINS` con esa URL (redespliega).

### 3) Usuarios semilla

En el primer arranque, `DataInitializer` crea automáticamente roles, permisos, planes, distritos y usuarios demo (`admin/Admin*123`, `supervisor1/Sup*123`, `back1/Back*123`, `agente1|agente2/Agente*123`). **Cambia estas contraseñas en producción.**

## 📁 Estructura del proyecto

```
E:\telco_ventas\
├── backend/       Spring Boot 3 (Java 21)
│   └── src/main/resources/
│       ├── application.properties            ← H2 (default) - puerto 8080
│       ├── application-mysql.properties      ← MySQL (perfil "mysql") - puerto 8081
│       ├── application-postgres.properties   ← PostgreSQL (perfil "postgres")
│       ├── schema.sql, schema-mysql.sql, schema-postgres.sql
│       └── data.sql, data-mysql.sql, data-postgres.sql  (catálogos + seed)
├── frontend/      Vite + React 18 (SPA) - puerto 5173
└── docs/
    ├── 01-diagrama-solucion.md
    ├── 02-arquitectura-y-despliegue.md
    ├── anexo-tablas-reporte.sql              ← SQL completo para phpMyAdmin
    └── Ventas-Telco.postman_collection.json
```

## 🛠️ Solución de problemas

| Problema | Solución |
|----------|----------|
| `Credenciales inválidas` al iniciar sesión | Verifica que Vite apunte al puerto correcto en `frontend/vite.config.js` (target `http://localhost:8081` con perfil MySQL). |
| Backend no conecta a MySQL | En Laragon `root` no tiene contraseña: deja `spring.datasource.password=` vacío en `application-mysql.properties`. |
| Error de parseo en los scripts `.ps1` | Los scripts usan solo ASCII (sin emojis) para compatibilidad con PowerShell 5.1. |
| `No se encontró JDK 21` | Ejecuta `powershell -ExecutionPolicy Bypass -File .\install-jdk21.ps1` o el script `setup-and-run.ps1` detecta el JDK portable en `backend/.tools/`. |

---

Documentación completa en la carpeta **[docs/](docs/)**.
