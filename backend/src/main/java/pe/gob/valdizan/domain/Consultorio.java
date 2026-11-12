package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "consultorios")
@Getter @Setter @NoArgsConstructor
public class Consultorio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConsultorio;

    @Column(nullable = false, length = 20)
    private String numeroPabellon;

    @Column(nullable = false, length = 20)
    private String codigoRoom;

    private boolean activo = true;

    public String getUbicacion() { return "Pabellón " + numeroPabellon + ", Room " + codigoRoom; }
}
