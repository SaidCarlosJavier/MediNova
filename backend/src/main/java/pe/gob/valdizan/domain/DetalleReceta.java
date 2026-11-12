package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalles_receta")
@Getter @Setter @NoArgsConstructor
public class DetalleReceta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_receta")
    private RecetaMedica receta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_medicamento")
    private Medicamento medicamento;

    @Column(nullable = false, length = 200) private String dosis;
    @Column(nullable = false) private int cantidad;
    @Column(nullable = false) private int cantidadDispensada;

    public int pendiente() { return cantidad - cantidadDispensada; }
}
