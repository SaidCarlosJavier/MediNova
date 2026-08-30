package com.sistema.sistema_hospitalario.dto.response;

import com.sistema.sistema_hospitalario.enums.AppointmentStatus;
import com.sistema.sistema_hospitalario.enums.ConsultationType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long specialtyId;
    private String specialtyName;
    private LocalDate date;
    private LocalTime time;
    private ConsultationType type;
    private Long locationId;
    private String locationName;
    private String reason;
    private AppointmentStatus status;
    private LocalDateTime createdAt;
}
