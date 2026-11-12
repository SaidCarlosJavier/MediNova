package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.Cargo;

@Entity
@Table(name = "administrativos")
@PrimaryKeyJoinColumn(name = "id_administrativo")
@Getter @Setter @NoArgsConstructor
public class Administrativo extends Usuario {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Cargo cargo;

    public Long getIdAdministrativo() { return getIdUsuario(); }
}
