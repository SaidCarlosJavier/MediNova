package com.sistema.sistema_hospitalario.dto.request;

import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.enums.ConsultationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorRequest {

    private Long userId;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotNull(message = "La especialidad es obligatoria")
    private Long specialtyId;

    private String photo;
    private Integer experience;
    private String education;
    private List<String> certifications;
    private String bio;
    private List<ConsultationType> consultationTypes;
    private Long locationId;
    private String professionalId;
    private String phone;
    private String email;
    private AccountStatus status;
    private List<ScheduleBlockRequest> schedule;
    private List<String> blockedDates;
}
