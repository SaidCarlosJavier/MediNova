package com.sistema.sistema_hospitalario.entity;

import com.sistema.sistema_hospitalario.enums.DocumentStatus;
import com.sistema.sistema_hospitalario.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private DocumentType type;

    private LocalDateTime date;

    private String size;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.AVAILABLE;

    @PrePersist
    protected void onCreate() {
        this.date = LocalDateTime.now();
    }
}
