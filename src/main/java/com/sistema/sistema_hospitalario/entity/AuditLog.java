package com.sistema.sistema_hospitalario.entity;

import com.sistema.sistema_hospitalario.enums.AuditModule;
import com.sistema.sistema_hospitalario.enums.AuditStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación débil por nombre, tal como se definió en el modelo de datos.
    // Se recomienda migrar a una relación @ManyToOne hacia User.id en el futuro.
    @Column(name = "user_name", nullable = false)
    private String user;

    private String action;

    @Enumerated(EnumType.STRING)
    private AuditModule module;

    private LocalDate date;

    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuditStatus status = AuditStatus.SUCCESS;

    @PrePersist
    protected void onCreate() {
        this.date = LocalDate.now();
        this.time = LocalTime.now();
    }
}
