package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.EstadoReceta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recetas_medicas", indexes = @Index(name = "idx_receta_estado", columnList = "estado"))
@Getter @Setter @NoArgsConstructor
public class RecetaMedica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReceta;

    @Column(length = 2000) private String indicaciones;
    @Column(nullable = false) private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoReceta estado = EstadoReceta.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_historial")
    private HistorialClinico historial;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleReceta> detalles = new ArrayList<>();

    public void agregar(DetalleReceta d) { d.setReceta(this); detalles.add(d); }
}
