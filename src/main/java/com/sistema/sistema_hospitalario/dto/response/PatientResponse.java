package com.sistema.sistema_hospitalario.dto.response;

import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.enums.Gender;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private Long id;
    private Long userId;
    private String name;
    private String nationalId;
    private LocalDate dob;
    private Gender gender;
    private String phone;
    private String email;
    private String address;
    private String photo;
    private LocalDateTime registered;
    private AccountStatus status;
    private List<String> allergies;
    private String bloodType;
}
