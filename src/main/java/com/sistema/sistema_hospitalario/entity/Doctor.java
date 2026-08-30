package com.sistema.sistema_hospitalario.entity;

import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.enums.ConsultationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    private String photo;

    @Column(name = "experience_years")
    private Integer experience;

    @Builder.Default
    private Double rating = 0.0;

    @Column(length = 500)
    private String education;

    @ElementCollection
    @CollectionTable(name = "doctor_certifications", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "certification")
    @Builder.Default
    private List<String> certifications = new ArrayList<>();

    @Column(length = 2000)
    private String bio;

    @ElementCollection(targetClass = ConsultationType.class)
    @CollectionTable(name = "doctor_consultation_types", joinColumns = @JoinColumn(name = "doctor_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type")
    @Builder.Default
    private List<ConsultationType> consultationTypes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "professional_id", unique = true, length = 50)
    private String professionalId;

    private String phone;

    private String email;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @ElementCollection
    @CollectionTable(name = "doctor_schedule", joinColumns = @JoinColumn(name = "doctor_id"))
    @Builder.Default
    private List<ScheduleBlock> schedule = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "doctor_blocked_dates", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "blocked_date")
    @Builder.Default
    private List<LocalDate> blockedDates = new ArrayList<>();
}
