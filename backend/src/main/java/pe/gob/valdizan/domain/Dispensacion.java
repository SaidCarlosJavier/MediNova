package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dispensaciones")
@Getter @Setter @NoArgsConstructor
public class Dispensacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDispensacion;

    @Column(nullable = false) private LocalDateTime fecha;

    @Column(precision = 10, scale = 2) private java.math.BigDecimal total = java.math.BigDecimal.ZERO;

    private boolean anulada;
    private LocalDateTime fechaAnulacion;
    @Column(length = 250) private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_receta")
    private RecetaMedica receta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_farmaceutico")
    private Administrativo farmaceutico;

    @OneToMany(mappedBy = "dispensacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleDispensacion> detalles = new ArrayList<>();

    public void agregar(DetalleDispensacion d) { d.setDispensacion(this); detalles.add(d); }
}
