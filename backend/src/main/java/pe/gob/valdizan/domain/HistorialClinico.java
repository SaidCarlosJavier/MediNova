package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historiales_clinicos", indexes = @Index(name = "idx_historial_paciente", columnList = "id_paciente"))
@Getter @Setter @NoArgsConstructor
public class HistorialClinico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHistorial;

    @Column(length = 2000) private String diagnostico;
    @Column(length = 2000) private String tratamiento;
    @Column(length = 2000) private String observaciones;
    @Column(nullable = false) private LocalDateTime fechaRegistro;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cita", unique = true)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_medico")
    private Medico medico;
}
