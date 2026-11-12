package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicos")
@PrimaryKeyJoinColumn(name = "id_medico")
@Getter @Setter @NoArgsConstructor
public class Medico extends Usuario {
    @Column(nullable = false, unique = true, length = 20)
    private String numeroColegiatura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_especialidad")
    private Especialidad especialidad;

    public Long getIdMedico() { return getIdUsuario(); }
}
