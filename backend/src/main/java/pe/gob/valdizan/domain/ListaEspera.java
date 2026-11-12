package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.EstadoEspera;

import java.time.LocalDateTime;

@Entity
@Table(name = "lista_espera", indexes = @Index(name = "idx_espera_especialidad", columnList = "id_especialidad,estado"))
@Getter @Setter @NoArgsConstructor
public class ListaEspera {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEspera;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_paciente")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_especialidad")
    private Especialidad especialidad;

    @Column(nullable = false) private LocalDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoEspera estado = EstadoEspera.EN_ESPERA;
}
