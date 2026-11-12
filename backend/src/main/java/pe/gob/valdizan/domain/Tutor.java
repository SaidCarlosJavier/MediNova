package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Tutor / apoderado: familiar o responsable legal que actúa en nombre del paciente. */
@Entity
@Table(name = "tutores")
@PrimaryKeyJoinColumn(name = "id_tutor")
@Getter @Setter @NoArgsConstructor
public class Tutor extends Usuario {
    @Column(length = 40)
    private String parentesco;

    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @OneToMany(mappedBy = "tutor")
    private List<Paciente> pacientes = new ArrayList<>();
}
