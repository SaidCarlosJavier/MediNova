package com.sistema.sistema_hospitalario.dto.request;

import com.sistema.sistema_hospitalario.enums.ConsultationType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @NotNull(message = "El paciente es obligatorio")
    private Long patientId;

    @NotNull(message = "El doctor es obligatorio")
    private Long doctorId;

    private Long specialtyId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime time;

    private ConsultationType type;
    private Long locationId;
    private String reason;
}
