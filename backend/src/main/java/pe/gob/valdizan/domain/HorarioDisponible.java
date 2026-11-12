package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.Turno;
import pe.gob.valdizan.exception.ApiException;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_disponibles",
        indexes = @Index(name = "idx_horario_busqueda", columnList = "fecha,id_medico,disponible"),
        uniqueConstraints = @UniqueConstraint(name = "uk_horario_medico_fecha_hora", columnNames = {"id_medico", "fecha", "hora_inicio"}))
@Getter @Setter @NoArgsConstructor
public class HorarioDisponible {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHorario;

    /** Bloqueo optimista: evita la doble reserva concurrente (RNF-08/17). */
    @Version
    private Long version;

    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false) private LocalTime horaInicio;
    @Column(nullable = false) private LocalTime horaFin;
    private boolean disponible = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_medico")
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_consultorio")
    private Consultorio consultorio;

    public void marcarComoOcupado() {
        if (!disponible) throw ApiException.conflicto("El horario seleccionado ya no está disponible");
        disponible = false;
    }

    public void liberar() { disponible = true; }
}
