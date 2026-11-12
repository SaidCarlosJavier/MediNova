package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.TipoMovimiento;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario", indexes = @Index(name = "idx_mov_medicamento", columnList = "id_medicamento"))
@Getter @Setter @NoArgsConstructor
public class MovimientoInventario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMovimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    /** ENTRADA/SALIDA: positivo. AJUSTE: con signo (+/-). */
    @Column(nullable = false) private int cantidad;
    @Column(nullable = false) private LocalDateTime fecha;
    @Column(length = 250) private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_medicamento")
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_usuario")
    private Usuario usuario;
}
