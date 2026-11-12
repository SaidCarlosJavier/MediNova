package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "medicamentos", indexes = @Index(name = "idx_medicamento_nombre", columnList = "nombre"))
@Getter @Setter @NoArgsConstructor
public class Medicamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMedicamento;

    @Column(nullable = false, length = 120) private String nombre;
    @Column(length = 120) private String principioActivo;
    @Column(length = 80) private String presentacion;
    @Column(nullable = false) private int stockActual;
    @Column(nullable = false) private int stockMinimo;
    private LocalDate fechaVencimiento;
    @Column(precision = 10, scale = 2) private BigDecimal precio;
    private boolean activo = true;
}
