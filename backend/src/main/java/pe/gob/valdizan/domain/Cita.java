package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.EstadoCita;
import pe.gob.valdizan.domain.Enums.Origen;
import pe.gob.valdizan.exception.ApiException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "citas",
        indexes = {
                @Index(name = "idx_cita_paciente", columnList = "id_paciente"),
                @Index(name = "idx_cita_medico", columnList = "id_medico"),
                @Index(name = "idx_cita_estado", columnList = "estado"),
                @Index(name = "idx_cita_horario", columnList = "id_horario")},
        // RF-35 / RNF-10: índice único lógico paciente+especialidad+fecha+hora (solo citas activas: clave_activa nula al cerrarse)
        uniqueConstraints = @UniqueConstraint(name = "uk_cita_clave_activa", columnNames = "clave_activa"))
@Getter @Setter @NoArgsConstructor
public class Cita {
    public static final Set<EstadoCita> MODIFICABLES =
            EnumSet.of(EstadoCita.RESERVADA, EstadoCita.CONFIRMADA, EstadoCita.REPROGRAMADA);

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCita;

    @Column(nullable = false)
    private LocalDateTime fechaHoraReserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Origen origen;

    private boolean checkIn;
    private boolean recordatorioEnviado;
    /** Se marca al registrarse el pago de la consulta (ver PagoService). */
    private boolean pagada;
    private Integer duracionReservaSeg;

    @Column(name = "clave_activa", length = 80)
    private String claveActiva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_paciente")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_medico")
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_horario")
    private HorarioDisponible horario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_consultorio")
    private Consultorio consultorio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_especialidad")
    private Especialidad especialidad;

    /** Quién reservó: paciente, tutor o recepcionista. */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_reservada_por")
    private Usuario reservadaPor;

    public boolean isModificable() { return MODIFICABLES.contains(estado); }

    public String construirClave(HorarioDisponible h) {
        return paciente.getIdUsuario() + "-" + especialidad.getIdEspecialidad() + "-" + h.getFecha() + "-" + h.getHoraInicio();
    }

    public String construirClave() { return construirClave(horario); }

    public void confirmar() { estado = EstadoCita.CONFIRMADA; }

    public void reprogramar(HorarioDisponible nuevo) {
        if (!isModificable()) throw ApiException.invalida("Solo se pueden reprogramar citas reservadas, confirmadas o reprogramadas");
        horario = nuevo;
        medico = nuevo.getMedico();
        consultorio = nuevo.getConsultorio();
        estado = EstadoCita.REPROGRAMADA;
        claveActiva = construirClave(nuevo);
        recordatorioEnviado = false;
    }

    public void cancelar() {
        if (!isModificable()) throw ApiException.invalida("Solo se pueden cancelar citas reservadas, confirmadas o reprogramadas");
        estado = EstadoCita.CANCELADA;
        claveActiva = null;
    }

    public void registrarCheckIn() {
        if (!isModificable()) throw ApiException.invalida("La cita no admite check-in en su estado actual");
        checkIn = true;
        estado = EstadoCita.ASISTIO;
    }

    public void marcarAtendida() {
        if (!isModificable() && estado != EstadoCita.ASISTIO)
            throw ApiException.invalida("Solo puede marcarse como atendida una cita vigente");
        estado = EstadoCita.ATENDIDA;
        claveActiva = null;
    }

    public void marcarNoAsistio() {
        estado = EstadoCita.NO_ASISTIO;
        claveActiva = null;
    }
}
