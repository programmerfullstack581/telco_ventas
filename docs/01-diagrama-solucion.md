# Diagrama de la Solución - Ventas Telco Fija Hogar

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Frontend                                   │
│                          Vite + React (SPA)                             │
│                     http://localhost:5173                               │
│  ┌──────────┐  ┌──────────────┐  ┌──────────────────┐                  │
│  │  Login   │→│  Dashboard    │→│  Router por Rol   │                  │
│  │  JWT     │  │  (vistas)    │  │  AGENTE          │                  │
│  │          │  │              │  │  BACKOFFICE      │                  │
│  │          │  │              │  │  SUPERVISOR      │                  │
│  └──────────┘  └──────────────┘  └──────────────────┘                  │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                │ HTTPS (JWT Bearer) / CORS *5173
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Backend Spring Boot 3                           │
│                           :8080 /api/v1                                 │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ CONTROLLER LAYER (@RestController)                                   │ │
│ │  · AuthController:       POST /auth/login  (JWT)                    │ │
│ │  · VentaController:      + métodos por rol (@PreAuthorize)          │ │
│ │                        - POST /ventas                   (AGENTE)    │ │
│ │                        - GET  /ventas/mis-ventas         (AGENTE)   │ │
│ │                        - GET  /ventas/pendientes   (BACKOFFICE)     │ │
│ │                        - POST /ventas/{id}/aprobar   (BACKOFFICE)   │ │
│ │                        - POST /ventas/{id}/rechazar  (BACKOFFICE)   │ │
│ │                        - GET  /ventas/equipo        (SUPERVISOR)    │ │
│ │                        - GET  /reportes/resumen     (SUPERVISOR)    │ │
│ └────────────────────────────────────┬────────────────────────────────┘ │
│                                      │                                  │
│ ┌────────────────────────────────────▼────────────────────────────────┐ │
│ │ SERVICE LAYER (@Service)                                             │ │
│ │  · AuthService:  autenticación + generación JWT                     │ │
│ │  · VentaService: reglas de negocio, validaciones, snapshots        │ │
│ └────────────────────────────────────┬────────────────────────────────┘ │
│                                      │                                  │
│ ┌────────────────────────────────────▼────────────────────────────────┐ │
│ │ SECURITY LAYER (Spring Security 6 + JWT)                             │ │
│ │  · JwtService          (JJWT 0.12.x - HS256)                        │ │
│ │  · JwtAuthFilter       (Bearer → UserDetails)                       │ │
│ │  · SecurityConfig      (FilterChain, CORS, roles, sin sesiones)     │ │
│ └────────────────────────────────────┬────────────────────────────────┘ │
│                                      │                                  │
│ ┌────────────────────────────────────▼────────────────────────────────┐ │
│ │ DATA LAYER (Spring Data JPA)                                         │ │
│ │  · UsuarioRepository   (findByUsername, findBySupervisorId)         │ │
│ │  · VentaRepository     (paginación, filtros, reportes por día)     │ │
│ └────────────────────────────────────┬────────────────────────────────┘ │
│                                      │                                  │
│ ┌────────────────────────────────────▼────────────────────────────────┐ │
│ │ EXCEPTION & VALIDATION                                               │ │
│ │  · GlobalExceptionHandler (@RestControllerAdvice)                   │ │
│ │       → JSON consistente: { timestamp, path, error, message }       │ │
│ │  · Jakarta Validation (@Valid) + BusinessException                  │ │
│ └────────────────────────────────────┬────────────────────────────────┘ │
│                                      │                                  │
│ ┌────────────────────────────────────▼────────────────────────────────┐ │
│ │ DATABASE  (H2 en memoria  ·  PostgreSQL en perfil "postgres")       │ │
│ │  Tablas:                                                             │ │
│ │   · usuarios (id, username, password_hash, rol, supervisor_id, ..)  │ │
│ │   · ventas   (snapshot del cliente  ← no hay tabla clientes)        │ │
│ │            (codigo_llamada UNIQUE, estado, motivo_rechazo, fechas)  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                                                                         │
│ · OpenAPI/Swagger:  http://localhost:8080/swagger-ui.html              │
│ · H2 Console:       http://localhost:8080/h2-console                   │
│ · Postman Coll.:    docs/Ventas-Telco.postman_collection.json          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Flujo Funcional

```
            [AGENTE]                                  [BACKOFFICE]
               │                                           │
  Crear venta ─┼─► POST /ventas (PENDIENTE)               │
               │                                           │
  Listar MIS   ├─► GET /ventas/mis-ventas                  │
  ventas       │                                           │
               │                                           │
               │                        PENDIENTES ◄──────┼─ GET /ventas/pendientes
               │                                           │
               │                APROBAR / RECHAZAR ───────┼─ POST /ventas/{id}/aprobar
               │                                           │  POST /ventas/{id}/rechazar
               │                                           │   · motivo_rechazo obligatorio
               │                                           │   · fecha_validacion automática
               │                                           │
               ▼                                           ▼
                        [SUPERVISOR]
                              │
          Ventas del equipo ──┼─► GET /ventas/equipo
          (agentes asociados) │   Filtros: estado, agenteId, desde, hasta
                              │
          Resumen / Reportes ─┼─► GET /ventas/reportes/resumen
                              │   · Total / Pendiente / Aprobadas / Rechazadas
                              │   · Monto total aprobadas
                              │   · Serie ventas_por_día (YYYY-MM-DD → count/monto)
                              │   · Filtros: día | mes(año,mes) | rango(desde,hasta)
```
