package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "triajes")
@Getter @Setter @NoArgsConstructor
public class Triaje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idTriaje;

    @Column(length = 20) private String presionArterial;
    private Double peso;
    @Column(length = 300) private String signosVitales;
    private LocalDateTime fechaRegistro;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cita", unique = true)
    private Cita cita;
}
