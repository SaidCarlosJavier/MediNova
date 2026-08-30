# Backend — Sistema Hospitalario (MediCare+)

Backend completo en **Spring Boot 4 / Java 21** para el sistema hospitalario, construido
sobre el modelo de datos entregado (`toma bbta`). Incluye entidades JPA, repositorios,
servicios, controladores REST y seguridad con **JWT**. Tus compañeros solo necesitan
conectar la base de datos (PostgreSQL) y ejecutar la app.

## Estructura de paquetes

```
com.sistema.sistema_hospitalario
├── entity           -> Entidades JPA (User, Patient, Doctor, Specialty, Location,
│                        Appointment, Consultation, MedicalDocument, Notification,
│                        AuditLog, + embebibles Vitals y ScheduleBlock)
├── enums             -> Role, AccountStatus, Gender, ConsultationType,
│                        AppointmentStatus, DocumentType, DocumentStatus,
│                        NotificationType, AuditModule, AuditStatus
├── repository        -> Interfaces JpaRepository por entidad
├── dto
│   ├── request        -> DTOs de entrada (validados con Bean Validation)
│   ├── response        -> DTOs de salida
│   └── common          -> ApiResponse<T> y ErrorResponse (envoltorios de respuesta)
├── service            -> Interfaces de la capa de negocio
├── service.impl        -> Implementaciones (@Service, @Transactional)
├── controller          -> Controladores REST (@RestController)
├── security             -> UserPrincipal (UserDetails) y UserDetailsServiceImpl
├── security.jwt          -> JwtService, JwtAuthenticationFilter,
│                            JwtAuthenticationEntryPoint
├── exception             -> Excepciones de negocio + GlobalExceptionHandler (@RestControllerAdvice)
└── config                -> SecurityConfig (JWT stateless, CORS, roles)
```

## Seguridad

- Autenticación **stateless** con JWT (header `Authorization: Bearer <token>`).
- Contraseñas cifradas con `BCryptPasswordEncoder`.
- Autorización por rol con `@PreAuthorize` (`ADMIN`, `DOCTOR`, `RECEPTIONIST`, `PATIENT`)
  usando `@EnableMethodSecurity`.
- Endpoints públicos: `/api/v1/auth/**` (registro y login).
- Manejo centralizado de errores (`GlobalExceptionHandler`) devolviendo siempre JSON
  consistente (`ApiResponse` / `ErrorResponse`).

## Endpoints principales

| Módulo | Base path | Notas |
|---|---|---|
| Auth | `/api/v1/auth` | `POST /register`, `POST /login` (públicos) |
| Usuarios | `/api/v1/users` | Solo `ADMIN` |
| Pacientes | `/api/v1/patients` | CRUD |
| Doctores | `/api/v1/doctors` | CRUD, filtro `?specialtyId=` |
| Especialidades | `/api/v1/specialties` | CRUD, `doctorCount` se recalcula automáticamente |
| Sedes | `/api/v1/locations` | CRUD |
| Citas | `/api/v1/appointments` | CRUD + `PATCH /{id}/status`, filtros por paciente/doctor, valida cruce de horario |
| Consultas | `/api/v1/consultations` | CRUD, puede cerrar una cita (`appointmentId`) |
| Documentos | `/api/v1/documents` | CRUD |
| Notificaciones | `/api/v1/notifications` | Por usuario, marcar leídas |
| Auditoría | `/api/v1/audit-logs` | Solo `ADMIN`, filtro por módulo |

Todas las respuestas siguen el formato:

```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "timestamp": "..."
}
```

## Configuración pendiente para tu equipo

En `src/main/resources/application.properties` ya están las claves necesarias
(`spring.datasource.*`, `app.jwt.secret`, `app.jwt.expiration-ms`). Solo deben:

1. Ajustar credenciales de PostgreSQL (o cambiar el driver si usan otro motor).
2. Cambiar `app.jwt.secret` por una clave segura en producción (idealmente por variable de entorno).
3. Ejecutar `mvn spring-boot:run` — con `spring.jpa.hibernate.ddl-auto=update`, Hibernate
   crea las tablas automáticamente a partir de las entidades.

## Notas de diseño

- La entidad `Document` del modelo se implementó como `MedicalDocument` para evitar
  conflicto de nombres con clases estándar de Java.
- `AuditLog.user` se dejó como `String` (relación débil por nombre), tal como indica
  el modelo de datos original, con un comentario recomendando migrarlo a `userId`.
- Thymeleaf y Bootstrap no se incluyen aquí porque el pedido fue **solo backend**;
  las dependencias de Thymeleaf ya están en el `pom.xml` original por si el equipo
  de frontend las necesita.
