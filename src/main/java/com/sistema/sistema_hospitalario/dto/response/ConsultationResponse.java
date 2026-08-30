package com.sistema.sistema_hospitalario.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long specialtyId;
    private String specialtyName;
    private LocalDateTime date;
    private String reason;
    private String symptoms;
    private VitalsResponse vitals;
    private String diagnosis;
    private String treatment;
    private List<String> medications;
    private String recommendations;
    private String notes;
    private LocalDate followUp;
}
