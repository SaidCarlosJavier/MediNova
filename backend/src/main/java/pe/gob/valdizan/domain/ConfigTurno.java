package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.Turno;

import java.time.LocalTime;

/** Configuración editable de turnos (RF-34): ventana horaria y duración de cada cita. */
@Entity
@Table(name = "config_turnos")
@Getter @Setter @NoArgsConstructor
public class ConfigTurno {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    private Turno turno;

    @Column(nullable = false) private LocalTime horaInicio;
    @Column(nullable = false) private LocalTime horaFin;
    @Column(nullable = false) private int duracionMinutos = 30;
}
