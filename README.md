# Sistema de Citas — Hospital Hermilio Valdizán

Backend Spring Boot 3 (Java 17, MySQL 8, JWT) + frontend React 18 (Vite, Bootstrap 5).

## Estructura
```
hospital-valdizan/
├── backend/   pom.xml · src/main/java/pe/gob/valdizan/{domain,repository,service,controller,dto,security,config,exception}
│              src/main/resources/{application*.properties, templates/index.html} · src/test/...
└── frontend/  package.json · vite.config.js · src/{components,pages/{paciente,recepcion,medico,farmacia,admin}}
```

## Cómo ejecutar
1. Requisitos: JDK 17, Maven 3.9+, Node 18+, MySQL 8 en `localhost:3306` (la BD `hospital_valdizan` se crea sola).
2. Variables opcionales: `DB_URL`, `DB_USER` (def. `root`), `DB_PASS` (def. `root`), `JWT_SECRET` (≥32 caracteres), `JWT_EXPIRATION_MS`, `CORS_ORIGINS`, `CORREO_HABILITADO`.
3. Backend: `cd backend && mvn spring-boot:run`
4. Frontend: `cd frontend && npm install && npm run dev`
5. URLs: API http://localhost:8080 · Swagger http://localhost:8080/swagger-ui.html · Health http://localhost:8080/actuator/health · Web http://localhost:5173
6. Pruebas: `cd backend && mvn test` (JUnit 5 + Mockito para `CitaService`; integración con H2 en modo MySQL).
7. Respaldo (RNF-15): `mysqldump -u root -p --single-transaction --routines hospital_valdizan > respaldo_$(date +%F).sql`
8. Producción: `SPRING_PROFILES_ACTIVE=prod` (ddl-auto=validate: cree antes el esquema, p. ej. ejecutando una vez en dev; Swagger y semilla desactivados; HTTPS en `application-prod.properties`, `server.ssl.*`).

## Credenciales de prueba (datos semilla)
| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | admin@valdizan.gob.pe | Admin2026* |
| Recepcionista | recepcion@valdizan.gob.pe | Valdizan2026* |
| Farmacéutico | farmacia@valdizan.gob.pe | Valdizan2026* |
| Médico (Psiquiatría) | psiquiatra@valdizan.gob.pe | Valdizan2026* |
| Médico (Psicología) | psicologo@valdizan.gob.pe | Valdizan2026* |
| Tutor (con paciente a cargo: Pedro Ramos) | tutor@valdizan.gob.pe | Valdizan2026* |
| Paciente independiente | paciente@valdizan.gob.pe | Valdizan2026* |

Todos los datos son ficticios. Cambie las contraseñas y `JWT_SECRET` antes de cualquier uso real.

## Novedades: farmacia, pagos y PDF

**Pasarela de pagos (simulada, sin cargos reales)** — tarjeta, Yape, Plin y efectivo (solo ventanilla/farmacia). Solo se guardan los últimos 4 dígitos de la tarjeta.
- Tarjeta aprobada: `4111 1111 1111 1111` · rechazada: `4000 0000 0000 0002` (cualquier vencimiento futuro y CVV de 3 dígitos).
- Yape/Plin: celular de 9 dígitos que empiece con 9 (terminado en `000` = rechazado).
- Comprobantes internos (sin validez tributaria): `B001-########` consultas, `B002-########` farmacia.
- Cancelar una cita pagada genera reembolso automático; anular una venta de farmacia devuelve el stock y deja la receta pendiente.
- Tarifa por especialidad: Administración → Configuración → Especialidades (S/ 0 = sin costo).

**PDF (en el navegador)** — boleta de farmacia y de consulta, reporte de cita (con diagnóstico y receta si el rol tiene acceso), receta, reporte de gestión (admin). Exportación CSV de agenda, pagos y reportes.

**Por rol**
- Paciente/tutor: pagar citas, «Mis pagos» (boletas), «Mis recetas», reporte PDF de cada cita.
- Recepción: «Caja» (cobros, anulación), cobro por ventanilla con efectivo.
- Médico: resumen del día, agenda por día/7/30 días con filtros y búsqueda, cancelar / no asistió, agendar seguimiento, «Mis pacientes», «Mi disponibilidad».
- Farmacia: cobro al dispensar, boleta PDF, «Ventas y boletas» con anulación.
- Admin: «Pagos e ingresos», ingresos en indicadores, reportes PDF/CSV.

> Con `ddl-auto=update` (perfil dev) las columnas/tablas nuevas se crean solas. En producción (`validate`) cree antes la tabla `pago` y las columnas nuevas (`especialidad.precio_consulta`, `cita.pagada`, `detalle_dispensacion.precio_unitario`, `dispensacion.total/anulada/fecha_anulacion/motivo_anulacion`).
> Nota: el backend no pudo compilarse en el entorno de generación (Maven Central bloqueado); el frontend sí compila (`npm run build`). Ejecute `mvn test` en su equipo y avíseme de cualquier error.

## Decisiones de diseño
- Anti-duplicado (RF-35): validación en `CitaService` + restricción única en BD sobre `clave_activa` (paciente+especialidad+fecha+hora, nula al cancelar/atender) → HTTP 409.
- Doble reserva concurrente: `@Version` en `HorarioDisponible`; stock de farmacia con bloqueo pesimista.
- Historial clínico: escritura solo del médico asignado; lectura del médico tratante, paciente o su tutor; cada acceso/modificación queda en `auditoria_log`.
- Correo/recuperación de contraseña: simulados por log (`CORREO_HABILITADO=true` + `spring.mail.*` para correo real).
- Recordatorios (8:00) e inasistencias (23:50) por `@Scheduled`, zona America/Lima.
