package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "pacientes", indexes = @Index(name = "idx_paciente_tutor", columnList = "id_tutor"))
@PrimaryKeyJoinColumn(name = "id_paciente")
@Getter @Setter @NoArgsConstructor
public class Paciente extends Usuario {
    @Column(length = 40)
    private String tipoSeguro;

    @Column(length = 20)
    private String telefonoContacto;

    private LocalDate fechaNacimiento;

    private boolean requiereTutor;

    /** Tutor / apoderado (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tutor")
    private Tutor tutor;

    public Long getIdPaciente() { return getIdUsuario(); }
}
