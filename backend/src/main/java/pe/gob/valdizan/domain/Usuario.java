package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Usuario abstracto (herencia JOINED). login/logout/perfil/recuperarContrasena viven en AuthService. */
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter @Setter @NoArgsConstructor
public abstract class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false, unique = true, length = 20)
    private String documentoIdentidad;

    @Column(nullable = false, length = 80)
    private String nombres;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(nullable = false)
    private String passwordHash;

    private boolean estado = true;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol")
    private Rol rol;

    // Recuperación de contraseña (RF-03)
    @Column(length = 80)
    private String tokenRecuperacion;
    private LocalDateTime tokenExpira;

    // Ley N.° 29733: consentimiento informado
    private boolean consentimientoDatos;
    private LocalDateTime fechaConsentimiento;

    public String getNombreCompleto() { return nombres + " " + apellidos; }
}
