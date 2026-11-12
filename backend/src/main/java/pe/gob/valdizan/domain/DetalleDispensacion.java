package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalles_dispensacion")
@Getter @Setter @NoArgsConstructor
public class DetalleDispensacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_dispensacion")
    private Dispensacion dispensacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_medicamento")
    private Medicamento medicamento;

    @Column(nullable = false) private int cantidad;

    /** Precio unitario al momento de la venta (no cambia si luego se edita el catálogo). */
    @Column(precision = 10, scale = 2) private java.math.BigDecimal precioUnitario;
}
