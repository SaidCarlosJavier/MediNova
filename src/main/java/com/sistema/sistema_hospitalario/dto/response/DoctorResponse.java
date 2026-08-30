package com.sistema.sistema_hospitalario.dto.response;

import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.enums.ConsultationType;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {

    private Long id;
    private Long userId;
    private String name;
    private Long specialtyId;
    private String specialtyName;
    private String photo;
    private Integer experience;
    private Double rating;
    private String education;
    private List<String> certifications;
    private String bio;
    private List<ConsultationType> consultationTypes;
    private Long locationId;
    private String locationName;
    private String professionalId;
    private String phone;
    private String email;
    private AccountStatus status;
    private List<ScheduleBlockResponse> schedule;
    private List<LocalDate> blockedDates;
}
