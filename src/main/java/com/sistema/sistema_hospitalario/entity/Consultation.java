package com.sistema.sistema_hospitalario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consultations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(length = 1000)
    private String reason;

    @Column(length = 2000)
    private String symptoms;

    @Embedded
    private Vitals vitals;

    @Column(length = 2000)
    private String diagnosis;

    @Column(length = 2000)
    private String treatment;

    @ElementCollection
    @CollectionTable(name = "consultation_medications", joinColumns = @JoinColumn(name = "consultation_id"))
    @Column(name = "medication")
    @Builder.Default
    private List<String> medications = new ArrayList<>();

    @Column(length = 2000)
    private String recommendations;

    @Column(length = 2000)
    private String notes;

    @Column(name = "follow_up")
    private LocalDate followUp;
}
