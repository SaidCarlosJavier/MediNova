package com.sistema.sistema_hospitalario.entity;

import com.sistema.sistema_hospitalario.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "specialties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String icon;

    @Column(length = 1000)
    private String description;

    @Column(name = "doctor_count")
    @Builder.Default
    private Integer doctorCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;
}
