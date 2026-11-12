package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "especialidades")
@Getter @Setter @NoArgsConstructor
public class Especialidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEspecialidad;

    @Column(nullable = false, unique = true, length = 80)
    private String nombreEspecialidad;

    @Column(length = 300)
    private String descripcion;

    private boolean activo = true;

    /** Tarifa de la consulta (S/). Cero o nulo = sin costo. */
    @Column(precision = 10, scale = 2)
    private BigDecimal precioConsulta = BigDecimal.ZERO;

    @OneToMany(mappedBy = "especialidad")
    private List<Medico> medicos = new ArrayList<>();
}
