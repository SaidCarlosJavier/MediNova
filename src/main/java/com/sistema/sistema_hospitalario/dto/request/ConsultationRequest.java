package com.sistema.sistema_hospitalario.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationRequest {

    @NotNull(message = "El paciente es obligatorio")
    private Long patientId;

    @NotNull(message = "El doctor es obligatorio")
    private Long doctorId;

    private Long specialtyId;
    private String reason;
    private String symptoms;
    private VitalsRequest vitals;
    private String diagnosis;
    private String treatment;
    private List<String> medications;
    private String recommendations;
    private String notes;
    private LocalDate followUp;

    // Cita de origen (opcional): si se envía, se marca como Completed
    private Long appointmentId;
}
