package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.NombreRol;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor
public class Rol {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private NombreRol nombreRol;

    public Rol(NombreRol nombreRol) { this.nombreRol = nombreRol; }
}
