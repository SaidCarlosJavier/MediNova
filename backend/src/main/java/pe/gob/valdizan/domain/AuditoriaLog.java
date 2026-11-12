package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** RNF-14: registro de accesos y modificaciones sobre el historial clínico. */
@Entity
@Table(name = "auditoria_log", indexes = @Index(name = "idx_auditoria_fecha", columnList = "fecha_hora"))
@Getter @Setter @NoArgsConstructor
public class AuditoriaLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLog;

    @Column(nullable = false, length = 30) private String accion;
    @Column(nullable = false) private LocalDateTime fechaHora;
    @Column(nullable = false, length = 50) private String entidad;
    @Column(length = 300) private String detalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_usuario")
    private Usuario usuario;
}
