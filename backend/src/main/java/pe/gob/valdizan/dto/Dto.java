package pe.gob.valdizan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import org.springframework.data.domain.Page;
import pe.gob.valdizan.domain.Enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** DTOs (records) de entrada y salida de la API. Mensajes de validación en español. */
public final class Dto {
    private Dto() {}

    // ----------------------------------------------------------- Genéricos
    public record MensajeDto(String mensaje) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(LocalDateTime timestamp, int status, String mensaje, Map<String, String> errores) {}

    public record PageDto<T>(List<T> contenido, int pagina, int tamanio, long total, int totalPaginas) {
        public static <T, E> PageDto<T> de(Page<E> p, Function<E, T> f) {
            return new PageDto<>(p.getContent().stream().map(f).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    // ----------------------------------------------------------- Autenticación
    public record LoginRequest(
            @NotBlank(message = "Ingrese su correo o documento de identidad") String identificador,
            @NotBlank(message = "Ingrese su contraseña") String password) {}

    public record RegistroRequest(
            @NotBlank(message = "Seleccione el tipo de registro") @Pattern(regexp = "PACIENTE|TUTOR", message = "Tipo de registro inválido") String tipo,
            @NotBlank(message = "Ingrese su documento de identidad") @Pattern(regexp = "[A-Za-z0-9]{8,12}", message = "El documento debe tener entre 8 y 12 caracteres alfanuméricos") String documentoIdentidad,
            @NotBlank(message = "Ingrese sus nombres") @Size(max = 80, message = "Máximo 80 caracteres") String nombres,
            @NotBlank(message = "Ingrese sus apellidos") @Size(max = 80, message = "Máximo 80 caracteres") String apellidos,
            @NotBlank(message = "Ingrese su correo electrónico") @Email(message = "Ingrese un correo válido") String correo,
            @NotBlank(message = "Ingrese una contraseña") @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres") String password,
            @AssertTrue(message = "Debe aceptar el tratamiento de sus datos personales (Ley N.° 29733)") boolean consentimiento,
            String tipoSeguro,
            @Pattern(regexp = "^$|[0-9+ ]{7,15}", message = "Teléfono inválido") String telefonoContacto,
            @Past(message = "La fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
            Boolean requiereTutor,
            String parentesco,
            @Pattern(regexp = "^$|[0-9+ ]{7,15}", message = "Teléfono inválido") String telefono,
            @Size(max = 200, message = "Máximo 200 caracteres") String direccion) {}

    public record RecuperarRequest(@NotBlank(message = "Ingrese su correo") @Email(message = "Ingrese un correo válido") String correo) {}

    public record RestablecerRequest(
            @NotBlank(message = "Falta el token de recuperación") String token,
            @NotBlank(message = "Ingrese la nueva contraseña") @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres") String nuevaPassword) {}

    public record PerfilRequest(
            @NotBlank(message = "Ingrese sus nombres") @Size(max = 80, message = "Máximo 80 caracteres") String nombres,
            @NotBlank(message = "Ingrese sus apellidos") @Size(max = 80, message = "Máximo 80 caracteres") String apellidos,
            String tipoSeguro,
            @Pattern(regexp = "^$|[0-9+ ]{7,15}", message = "Teléfono inválido") String telefonoContacto,
            @Past(message = "La fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
            String parentesco,
            @Pattern(regexp = "^$|[0-9+ ]{7,15}", message = "Teléfono inválido") String telefono,
            @Size(max = 200, message = "Máximo 200 caracteres") String direccion,
            @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres") String nuevaPassword) {}

    @Builder
    public record UsuarioDto(Long id, String documentoIdentidad, String nombres, String apellidos, String nombreCompleto,
                             String correo, String rol, boolean estado, String tipo,
                             String tipoSeguro, String telefonoContacto, LocalDate fechaNacimiento, Boolean requiereTutor,
                             Long tutorId, String tutor, String parentesco, String telefono, String direccion,
                             String numeroColegiatura, Long especialidadId, String especialidad, String cargo) {}

    public record AuthResponse(String token, UsuarioDto usuario) {}

    // ----------------------------------------------------------- Catálogo
    public record EspecialidadDto(Long id, String nombre, String descripcion, boolean activo, BigDecimal precioConsulta) {}

    public record EspecialidadRequest(
            @NotBlank(message = "Ingrese el nombre de la especialidad") @Size(max = 80, message = "Máximo 80 caracteres") String nombre,
            @Size(max = 300, message = "Máximo 300 caracteres") String descripcion,
            @DecimalMin(value = "0.00", message = "La tarifa no puede ser negativa") @DecimalMax(value = "9999.99", message = "Tarifa fuera de rango") BigDecimal precioConsulta,
            Boolean activo) {}

    public record ConsultorioDto(Long id, String numeroPabellon, String codigoRoom, String ubicacion, boolean activo) {}

    public record ConsultorioRequest(
            @NotBlank(message = "Ingrese el número de pabellón") @Size(max = 20, message = "Máximo 20 caracteres") String numeroPabellon,
            @NotBlank(message = "Ingrese el código de room") @Size(max = 20, message = "Máximo 20 caracteres") String codigoRoom,
            Boolean activo) {}

    public record MedicoDto(Long id, String nombreCompleto, String numeroColegiatura, Long especialidadId, String especialidad) {}

    public record ConfigTurnoDto(
            @NotNull(message = "Seleccione el turno") Turno turno,
            @NotNull(message = "Ingrese la hora de inicio") LocalTime horaInicio,
            @NotNull(message = "Ingrese la hora de fin") LocalTime horaFin,
            @Min(value = 10, message = "La duración mínima es de 10 minutos") @Max(value = 120, message = "La duración máxima es de 120 minutos") int duracionMinutos) {}

    // ----------------------------------------------------------- Horarios
    @Builder
    public record HorarioDto(Long idHorario, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, boolean disponible, Turno turno,
                             Long medicoId, String medico, Long especialidadId, String especialidad,
                             Long consultorioId, String pabellon, String room) {}

    public record GenerarHorariosRequest(
            @NotNull(message = "Seleccione el médico") Long medicoId,
            @NotNull(message = "Seleccione el consultorio") Long consultorioId,
            @NotNull(message = "Ingrese la fecha de inicio") LocalDate desde,
            @NotNull(message = "Ingrese la fecha de fin") LocalDate hasta,
            @NotNull(message = "Seleccione el turno") Turno turno) {}

    // ----------------------------------------------------------- Citas
    public record ReservaRequest(
            @NotNull(message = "Seleccione el paciente") Long pacienteId,
            @NotNull(message = "Seleccione un horario") Long horarioId,
            Origen origen,
            Integer duracionReservaSeg) {}

    public record ReprogramarRequest(@NotNull(message = "Seleccione el nuevo horario") Long horarioId) {}

    @Builder
    public record CitaDto(Long idCita, LocalDateTime fechaHoraReserva, EstadoCita estado, Origen origen, boolean checkIn,
                          LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                          Long pacienteId, String paciente, String pacienteDocumento,
                          Long medicoId, String medico, Long especialidadId, String especialidad,
                          Long consultorioId, String pabellon, String room, String ubicacion,
                          String reservadaPor, boolean modificable, BigDecimal precio, boolean pagada) {}

    public record TriajeRequest(
            @Size(max = 20, message = "Máximo 20 caracteres") String presionArterial,
            @DecimalMin(value = "0.0", message = "El peso no puede ser negativo") @DecimalMax(value = "500.0", message = "Peso fuera de rango") Double peso,
            @Size(max = 300, message = "Máximo 300 caracteres") String signosVitales) {}

    public record TriajeDto(Long idTriaje, Long citaId, String presionArterial, Double peso, String signosVitales, LocalDateTime fechaRegistro) {}

    // ----------------------------------------------------------- Pacientes / tutor
    public record TutorRequest(
            @NotBlank(message = "Ingrese el documento del tutor") @Pattern(regexp = "[A-Za-z0-9]{8,12}", message = "El documento debe tener entre 8 y 12 caracteres alfanuméricos") String documentoIdentidad,
            @NotBlank(message = "Ingrese los nombres del tutor") @Size(max = 80, message = "Máximo 80 caracteres") String nombres,
            @NotBlank(message = "Ingrese los apellidos del tutor") @Size(max = 80, message = "Máximo 80 caracteres") String apellidos,
            @NotBlank(message = "Ingrese el correo del tutor") @Email(message = "Ingrese un correo válido") String correo,
            @NotBlank(message = "Indique el parentesco") String parentesco,
            @Pattern(regexp = "^$|[0-9+ ]{7,15}", message = "Teléfono inválido") String telefono,
            @Size(max = 200, message = "Máximo 200 caracteres") String direccion) {}

    public record PacienteRequest(
            @NotBlank(message = "Ingrese el documento de identidad") @Pattern(regexp = "[A-Za-z0-9]{8,12}", message = "El documento debe tener entre 8 y 12 caracteres alfanuméricos") String documentoIdentidad,
            @NotBlank(message = "Ingrese los nombres") @Size(max = 80, message = "Máximo 80 caracteres") String nombres,
            @NotBlank(message = "Ingrese los apellidos") @Size(max = 80, message = "Máximo 80 caracteres") String apellidos,
            @Email(message = "Ingrese un correo válido") String correo,
            String tipoSeguro,
            @Pattern(regexp = "^$|[0-9+ ]{7,15}", message = "Teléfono inválido") String telefonoContacto,
            @NotNull(message = "Ingrese la fecha de nacimiento") @Past(message = "La fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
            boolean requiereTutor,
            @AssertTrue(message = "Debe registrarse el consentimiento informado (Ley N.° 29733)") boolean consentimiento,
            @Valid TutorRequest tutor) {}

    @Builder
    public record PacienteDto(Long id, String documentoIdentidad, String nombres, String apellidos, String nombreCompleto, String correo,
                              String tipoSeguro, String telefonoContacto, LocalDate fechaNacimiento, boolean requiereTutor,
                              Long tutorId, String tutor) {}

    public record RegistroPacienteResponse(PacienteDto paciente, String passwordTemporalTutor) {}

    /** La fecha de nacimiento actúa como segundo dato de verificación al vincular un paciente existente. */
    public record VincularRequest(
            @NotBlank(message = "Ingrese el documento del paciente") String documentoIdentidad,
            @NotNull(message = "Ingrese la fecha de nacimiento del paciente") LocalDate fechaNacimiento) {}

    // ----------------------------------------------------------- Lista de espera y notificaciones
    public record ListaEsperaRequest(@NotNull(message = "Seleccione el paciente") Long pacienteId,
                                     @NotNull(message = "Seleccione la especialidad") Long especialidadId) {}

    public record ListaEsperaDto(Long id, Long pacienteId, String paciente, String pacienteDocumento,
                                 Long especialidadId, String especialidad, LocalDateTime fechaRegistro, EstadoEspera estado) {}

    public record NotificacionDto(Long id, String mensaje, TipoNotificacion tipo, LocalDateTime fechaEnvio, boolean leido) {}

    // ----------------------------------------------------------- Historial y recetas
    public record DetalleRecetaRequest(
            @NotNull(message = "Seleccione el medicamento") Long medicamentoId,
            @NotBlank(message = "Ingrese la dosis") @Size(max = 200, message = "Máximo 200 caracteres") String dosis,
            @NotNull(message = "Ingrese la cantidad") @Min(value = 1, message = "La cantidad mínima es 1") @Max(value = 1000, message = "Cantidad fuera de rango") Integer cantidad) {}

    public record HistorialRequest(
            @NotBlank(message = "Ingrese el diagnóstico") @Size(max = 2000, message = "Máximo 2000 caracteres") String diagnostico,
            @Size(max = 2000, message = "Máximo 2000 caracteres") String tratamiento,
            @Size(max = 2000, message = "Máximo 2000 caracteres") String observaciones,
            @Size(max = 2000, message = "Máximo 2000 caracteres") String indicaciones,
            @Valid List<DetalleRecetaRequest> detalles) {}

    @Builder
    public record DetalleRecetaDto(Long idDetalle, Long medicamentoId, String medicamento, String presentacion, String dosis,
                                   int cantidad, int cantidadDispensada, int stockActual, BigDecimal precioUnitario) {}

    @Builder
    public record RecetaDto(Long idReceta, LocalDateTime fecha, String indicaciones, EstadoReceta estado, Long historialId,
                            String paciente, String pacienteDocumento, String medico, List<DetalleRecetaDto> detalles) {}

    @Builder
    public record HistorialDto(Long idHistorial, Long citaId, LocalDateTime fechaRegistro, LocalDate fechaCita,
                               String diagnostico, String tratamiento, String observaciones,
                               String medico, String especialidad, Long pacienteId, String paciente,
                               RecetaDto receta, TriajeDto triaje) {}

    // ----------------------------------------------------------- Farmacia
    public record MedicamentoRequest(
            @NotBlank(message = "Ingrese el nombre del medicamento") @Size(max = 120, message = "Máximo 120 caracteres") String nombre,
            @Size(max = 120, message = "Máximo 120 caracteres") String principioActivo,
            @Size(max = 80, message = "Máximo 80 caracteres") String presentacion,
            @NotNull(message = "Ingrese el stock actual") @Min(value = 0, message = "El stock no puede ser negativo") Integer stockActual,
            @NotNull(message = "Ingrese el stock mínimo") @Min(value = 0, message = "El stock mínimo no puede ser negativo") Integer stockMinimo,
            @NotNull(message = "Ingrese la fecha de vencimiento") LocalDate fechaVencimiento,
            @DecimalMin(value = "0.00", message = "El precio no puede ser negativo") BigDecimal precio,
            Boolean activo) {}

    public record MedicamentoDto(Long idMedicamento, String nombre, String principioActivo, String presentacion,
                                 int stockActual, int stockMinimo, LocalDate fechaVencimiento, BigDecimal precio,
                                 boolean activo, boolean stockBajo, boolean porVencer) {}

    public record MovimientoRequest(
            @NotNull(message = "Seleccione el tipo de movimiento") TipoMovimiento tipo,
            @NotNull(message = "Ingrese la cantidad") Integer cantidad,
            @Size(max = 250, message = "Máximo 250 caracteres") String motivo) {}

    public record MovimientoDto(Long id, TipoMovimiento tipo, int cantidad, LocalDateTime fecha, String motivo, Long medicamentoId, String medicamento, String usuario) {}

    public record AlertasDto(List<MedicamentoDto> stockBajo, List<MedicamentoDto> porVencer) {}

    public record ItemDispensar(@NotNull(message = "Falta el detalle de receta") Long idDetalle,
                                @NotNull(message = "Ingrese la cantidad") @Min(value = 1, message = "La cantidad mínima es 1") Integer cantidad) {}

    /** items vacío = dispensar todo lo pendiente. pago es obligatorio si el total es mayor a cero. */
    public record DispensarRequest(@Valid List<ItemDispensar> items, @Valid PagoRequest pago) {}

    public record AnularRequest(@NotBlank(message = "Indique el motivo de la anulación") @Size(max = 250, message = "Máximo 250 caracteres") String motivo) {}

    @Builder
    public record LineaDispensacionDto(Long idMedicamento, String medicamento, String presentacion, int cantidad,
                                       BigDecimal precioUnitario, BigDecimal subtotal) {}

    @Builder
    public record DispensacionDto(Long idDispensacion, LocalDateTime fecha, BigDecimal total, boolean anulada, String motivoAnulacion,
                                  String farmaceutico, Long recetaId, String paciente, String pacienteDocumento, String medico,
                                  RecetaDto receta, List<LineaDispensacionDto> lineas, PagoDto pago) {}

    // ----------------------------------------------------------- Pagos (pasarela simulada)
    /** Datos del pago. La tarjeta se valida y se descarta: jamás se persiste el número completo ni el CVV. */
    public record PagoRequest(
            @NotNull(message = "Seleccione el método de pago") MetodoPago metodo,
            @Size(max = 25, message = "Número de tarjeta inválido") String numeroTarjeta,
            @Size(max = 60, message = "Máximo 60 caracteres") String titular,
            @Size(max = 5, message = "Formato MM/AA") String vencimiento,
            @Size(max = 4, message = "CVV inválido") String cvv,
            @Size(max = 15, message = "Celular inválido") String celular,
            @DecimalMin(value = "0.00", message = "Monto inválido") BigDecimal montoRecibido) {}

    @Builder
    public record PagoDto(Long idPago, TipoPago tipo, MetodoPago metodo, EstadoPago estado, BigDecimal monto, String referencia,
                          String comprobante, LocalDateTime fecha, String concepto, Long citaId, Long dispensacionId,
                          Long pacienteId, String paciente, String pacienteDocumento, String pagador, String tarjetaUltimos4,
                          BigDecimal montoRecibido, BigDecimal vuelto, String motivoAnulacion) {}

    public record DiaIngreso(LocalDate fecha, BigDecimal monto) {}

    public record ResumenIngresosDto(LocalDate desde, LocalDate hasta, BigDecimal total, long cantidadPagos, long anulados,
                                     Map<String, BigDecimal> porMetodo, Map<String, BigDecimal> porTipo, List<DiaIngreso> porDia) {}

    // ----------------------------------------------------------- Médico
    public record MedicoPacienteDto(Long id, String nombreCompleto, String documentoIdentidad, LocalDate ultimaCita,
                                    LocalDate proximaCita, long totalCitas, long atendidas) {}

    public record MedicoResumenDto(long citasHoy, long atendidasHoy, long pendientesHoy, long citasSemana, long pacientesTotales,
                                   LocalDate hoy) {}

    // ----------------------------------------------------------- Administración y reportes
    public record UsuarioAdminRequest(
            @NotBlank(message = "Seleccione el tipo de usuario") @Pattern(regexp = "MEDICO|RECEPCIONISTA|FARMACEUTICO|ADMINISTRADOR", message = "Tipo de usuario inválido") String tipo,
            @NotBlank(message = "Ingrese el documento de identidad") @Pattern(regexp = "[A-Za-z0-9]{8,12}", message = "El documento debe tener entre 8 y 12 caracteres alfanuméricos") String documentoIdentidad,
            @NotBlank(message = "Ingrese los nombres") @Size(max = 80, message = "Máximo 80 caracteres") String nombres,
            @NotBlank(message = "Ingrese los apellidos") @Size(max = 80, message = "Máximo 80 caracteres") String apellidos,
            @NotBlank(message = "Ingrese el correo") @Email(message = "Ingrese un correo válido") String correo,
            @NotBlank(message = "Ingrese una contraseña") @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres") String password,
            String numeroColegiatura,
            Long especialidadId) {}

    public record CambiarEstadoRequest(@NotNull(message = "Indique el estado") Boolean estado) {}

    public record CambiarCargoRequest(@NotNull(message = "Seleccione el cargo") Cargo cargo) {}

    public record ReporteCitasDto(LocalDate desde, LocalDate hasta, long total, Map<String, Long> porEstado,
                                  long atendidas, long canceladas, long reprogramadas) {}

    public record InasistenciaFila(String especialidad, long total, long noAsistio, double porcentaje) {}

    public record IndicadoresDto(long totalCitas, long atendidas, long canceladas, long reprogramadas, long noAsistio,
                                 double porcentajeInasistencia, Double tiempoPromedioReservaSeg) {}

    public record AuditoriaDto(Long id, String accion, LocalDateTime fechaHora, String entidad, String detalle, String usuario) {}
}
