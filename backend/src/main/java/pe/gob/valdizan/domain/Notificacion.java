package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.TipoNotificacion;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones", indexes = @Index(name = "idx_notif_destinatario", columnList = "id_destinatario,leido"))
@Getter @Setter @NoArgsConstructor
public class Notificacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotificacion;

    @Column(nullable = false, length = 500) private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoNotificacion tipo;

    private LocalDateTime fechaEnvio;
    private boolean leido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_destinatario")
    private Usuario destinatario;

    public Notificacion(Usuario destinatario, TipoNotificacion tipo, String mensaje) {
        this.destinatario = destinatario;
        this.tipo = tipo;
        this.mensaje = mensaje.length() > 500 ? mensaje.substring(0, 497) + "..." : mensaje;
    }

    public void enviar() { fechaEnvio = LocalDateTime.now(); leido = false; }

    public void marcarLeido() { leido = true; }
}
