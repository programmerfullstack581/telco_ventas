# Decisiones Técnicas y Guía de Despliegue Local

## 1. Decisiones Técnicas

### Stack Backend
- **Java 21 + Spring Boot 3.2.x**: LTS actual de Java y versión estable de Spring Boot con soporte para Jakarta EE 9+ y Spring Security 6.
- **Spring Web / Spring Data JPA**: stack estándar y maduro para APIs REST + persistencia transaccional.
- **Spring Security 6 + JJWT 0.12.x**:
  - `@PreAuthorize` a nivel de método para control por rol (ADMIN hereda permisos).
  - Filtro `OncePerRequestFilter` para autenticación Bearer.
  - Password hashing **BCrypt** (strength 10 por defecto) mediante `PasswordEncoder`.
- **Validación Jakarta (`@Valid`)**: DNI 8/11 dígitos, teléfono 9 dígitos, campos no vacíos, monto > 0.
- **SpringDoc OpenAPI (Swagger UI)**: Documentación automática con esquema de seguridad Bearer-JWT.
- **Bases de datos** (3 perfiles, mismos datos):
  - **H2** en memoria (perfil por defecto): arranque cero-configuración ideal para demo o revisión rápida.
  - **MySQL 8.x** perfil `mysql`: **recomendado para el taller / entrega final**. Driver `mysql-connector-j`, tablas con `AUTO_INCREMENT`, `InnoDB`, `utf8mb4_unicode_ci`. Crea BD automáticamente si no existe (`createDatabaseIfNotExist=true`).
  - **PostgreSQL** perfil `postgres`: para entornos persistentes (schema separado con `BIGSERIAL`).
- **Lombok**: reduce boilerplate de entidades/DTOs (@Data, @Builder, @RequiredArgsConstructor).
- **GlobalExceptionHandler**:
  - Respuesta JSON consistente: `{ timestamp, path, error, message }`.
  - Trata `ResourceNotFoundException` (404), `BusinessException` (400), `MethodArgumentNotValidException` (422), `BadCredentialsException` (401), `AccessDeniedException` (403) y errores genéricos (500).
- **DataInitializer (CommandLineRunner)**:
  - Seed de usuarios y 8 ventas de ejemplo (mezcla PENDIENTE/APROBADA/RECHAZADA).
  - Los *passwords se codifican en runtime* con BCrypt para evitar hashes inconsistentes.
  - `data.sql` existe vacío para satisfacer `spring.sql.init` sin errores.

### Stack Frontend
- **Vite 5 + React 18 + React Router 6**: SPA moderna con arranque instantáneo.
- **Axios**: interceptor para inyectar JWT desde `localStorage` y redirigir a `/login` en 401.
- **Router protegido** (`PrivateRoute` + redirección por rol):
  - `ADMIN` y `SUPERVISOR` → `/supervisor`
  - `BACKOFFICE` → `/backoffice`
  - `AGENTE` → `/agente`
- **UI sin dependencias CSS extra**: estilos inline en `index.html` (gradientes, badges, tablas, stats, mini bar-chart).
- **Proxy Vite**: `/api → http://localhost:8080` para evitar problemas CORS en dev.

### Arquitectura
- **Snapshot de cliente en Venta**: se cumplió el requisito de no crear tabla `clientes`; todos los campos personales (DNI, nombre, teléfono, dirección, planes) se guardan dentro de la fila de `ventas`, garantizando inmutabilidad histórica.
- **Jerarquía de supervisión**: `usuarios.supervisor_id` referencia recursivamente a `usuarios.id`. El endpoint `/equipo` filtra por `supervisor_id = id del supervisor autenticado` (opcionalmente por `agenteId` individual, validando que pertenezca al equipo).
- **CORS**: configurado en `SecurityConfig` con origen `http://localhost:5173` (variable `cors.allowed-origins`).

---

## 2. Guía de Despliegue Local (Windows y Linux)

### Requisitos previos
| Herramienta  | Versión mínima | Instalación Windows | Instalación Linux (Debian/Ubuntu) |
|--------------|----------------|---------------------|------------------------------------|
| JDK          | 21 LTS         | https://adoptium.net (`JAVA_HOME` + `Path`) | `sudo apt install openjdk-21-jdk` |
| Maven        | 3.9+           | Incluido en el wrapper `./mvnw` (o `choco install maven`) | `sudo apt install maven` |
| Node.js      | 20 LTS         | https://nodejs.org (incluye npm) | `curl -fsSL https://deb.nodesource.com/setup_20.x \| sudo -E bash - && sudo apt install -y nodejs` |
| MySQL 8      | 8.0+ (recomendado) | https://dev.mysql.com/downloads/installer/ · o **XAMPP/WAMP** con MySQL | `sudo apt install mysql-server` |
| PostgreSQL*  | 15+ (opcional) | https://www.postgresql.org/download/windows | `sudo apt install postgresql postgresql-contrib` |

> \* MySQL es el perfil recomendado para la entrega. PostgreSQL y H2 son alternativas.

---

### Opción A: Arrancar con H2 (demo 0-config)

#### 1. Backend
```bash
# Windows (PowerShell)
cd backend
mvn spring-boot:run

# Linux / macOS
cd backend
./mvnw spring-boot:run
```

Esperar el mensaje **Started VentasTelcoApplication in X.XXX seconds**.

#### 2. Frontend
```bash
cd frontend
npm install
npm run dev
```

---

### ✅ Opción B: Arrancar con MySQL (RECOMENDADO PARA EL TALLER — todo se guarda en BD)

#### 1. Instalar y preparar MySQL

Windows:
1. Instala **MySQL Community Server 8.x** con `root` / `root` (o tu credencial).
2. *(Opcional)* Abre MySQL Workbench / línea de comandos y crea la BD (si no quieres que el driver la cree sola):
   ```sql
   CREATE DATABASE telco_ventas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

Linux (Ubuntu/Debian):
```bash
sudo apt install mysql-server -y
sudo mysql
# Dentro de MySQL:
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root';
CREATE DATABASE telco_ventas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
FLUSH PRIVILEGES;
EXIT;
```

#### 2. (Opcional) Ajustar credenciales
Edita [application-mysql.properties](file:///C:/Users/DESINTEGRACION/Documents/trae_projects/prueba_taller_desarrollador/backend/src/main/resources/application-mysql.properties) si tu usuario/password es distinto:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/telco_ventas?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_CONTRASENA
```

#### 3. Arrancar backend con perfil **mysql**
```bash
# Windows PowerShell
cd backend
$env:SPRING_PROFILES_ACTIVE="mysql"
mvn spring-boot:run

# Linux / macOS
cd backend
SPRING_PROFILES_ACTIVE=mysql ./mvnw spring-boot:run
```

Qué pasa en este paso (automático):
1. Hibernate ejecuta `schema-mysql.sql` → crea tablas `usuarios` y `ventas` con `InnoDB`, `utf8mb4` e índices.
2. `DataInitializer` detecta que `usuarios` está vacía → inserta **5 usuarios** con BCrypt + **8 ventas seed** (PENDIENTE/APROBADA/RECHAZADA).
3. Aplicación lista en el puerto 8080.

#### 4. Verificar datos en MySQL
```sql
USE telco_ventas;
SELECT id, username, rol, supervisor_id FROM usuarios;
SELECT id, nombre_cliente, estado, monto, fecha_registro FROM ventas ORDER BY fecha_registro DESC;
```

#### 5. Arrancar frontend
```bash
cd frontend && npm install && npm run dev
```

---

### Opción C: Arrancar con PostgreSQL (BD persistente alternativa)

#### 1. Crear BD y usuario
```sql
-- Ejecutar en psql / pgAdmin
CREATE USER telco WITH PASSWORD 'telco123';
CREATE DATABASE telco_ventas OWNER telco;
GRANT ALL PRIVILEGES ON DATABASE telco_ventas TO telco;
```

#### 2. Editar `backend/src/main/resources/application-postgres.properties`
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/telco_ventas
spring.datasource.username=telco
spring.datasource.password=telco123
```

#### 3. Arrancar backend con perfil postgres
```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="postgres"
./mvnw.cmd spring-boot:run

# Linux / macOS
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

#### 4. Frontend (igual que opción A)
```bash
cd frontend && npm install && npm run dev
```

---

## 3. URLs útiles

| Servicio         | URL                                                       |
|------------------|-----------------------------------------------------------|
| Frontend SPA     | http://localhost:5173                                     |
| API REST base    | http://localhost:8080/api/v1                              |
| Swagger UI       | http://localhost:8080/swagger-ui.html                     |
| OpenAPI JSON     | http://localhost:8080/v3/api-docs                         |
| H2 Console (H2)  | http://localhost:8080/h2-console  (JDBC: `jdbc:h2:mem:telco`, user: `sa`, pass vacía) |

---

## 4. Usuarios de prueba

| Usuario       | Contraseña  | Rol          | Notas                          |
|---------------|-------------|--------------|--------------------------------|
| admin         | Admin*123   | ADMIN        | Acceso total                   |
| agente1       | Agente*123  | AGENTE       | Bajo supervisor1 + 5 ventas    |
| agente2       | Agente*123  | AGENTE       | Bajo supervisor1 + 3 ventas    |
| back1         | Back*123    | BACKOFFICE   | Valida pendientes              |
| supervisor1   | Sup*123     | SUPERVISOR   | Reportes sobre agente1,agente2 |

---

## 5. Probar el flujo completo

1. **Login como agente1** y **Registrar 2 ventas** (cod. llamada único).
2. **Login como back1**, **aprobar 1 venta** y **rechazar la otra** con motivo ≥ 5 caracteres.
3. **Login como supervisor1**, consultar **ventas de equipo** (filtros) y **resumen por rango**.
4. Abrir **Swagger UI**, autenticar con `Bearer <token>` y ejecutar endpoints.
5. Importar `docs/Ventas-Telco.postman_collection.json` en Postman/Insomnia y correr la suite ordenada.

---

## 6. Estructura de carpetas

```
prueba_taller_desarrollador/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/telco/ventas/
│       │   ├── VentasTelcoApplication.java
│       │   ├── config/         (SecurityConfig, AppConfig, OpenApiConfig, DataInitializer)
│       │   ├── controller/     (AuthController, VentaController)
│       │   ├── dto/            (Login/CreateVenta/Rechazar/VentaResponse/Resumen)
│       │   ├── entity/         (Usuario, Venta, Rol, EstadoVenta)
│       │   ├── exception/      (GlobalExceptionHandler + custom)
│       │   ├── repository/     (UsuarioRepository, VentaRepository)
│       │   ├── security/       (JwtService, JwtAuthenticationFilter)
│       │   └── service/        (AuthService, VentaService)
│       └── resources/
│           ├── application.properties         (H2 por defecto)
│           ├── application-postgres.properties
│           ├── schema.sql / schema-postgres.sql
│           └── data.sql  / data-postgres.sql
├── frontend/
│   ├── index.html, vite.config.js, package.json
│   └── src/
│       ├── main.jsx, App.jsx
│       ├── components/Navbar.jsx
│       ├── context/AuthContext.jsx
│       ├── pages/  (Login, AgenteDashboard, BackofficeDashboard, SupervisorDashboard)
│       └── services/api.js
└── docs/
    ├── 01-diagrama-solucion.md
    ├── 02-arquitectura-y-despliegue.md   ← este archivo
    └── Ventas-Telco.postman_collection.json
```

---

## 7. Comandos útiles

```bash
# Build backend JAR
cd backend && ./mvnw clean package
java -jar target/ventas-telco-1.0.0.jar

# Build frontend producción
cd frontend && npm run build
# salida en dist/ → servir con nginx/caddy o `npm run preview`

# Tests (si se agregan)
cd backend && ./mvnw test
```
