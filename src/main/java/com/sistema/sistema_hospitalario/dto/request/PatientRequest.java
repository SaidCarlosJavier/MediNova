package com.sistema.sistema_hospitalario.dto.request;

import com.sistema.sistema_hospitalario.enums.AccountStatus;
import com.sistema.sistema_hospitalario.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequest {

    private Long userId;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El documento de identidad es obligatorio")
    private String nationalId;

    private LocalDate dob;
    private Gender gender;
    private String phone;
    private String email;
    private String address;
    private String photo;
    private AccountStatus status;
    private List<String> allergies;
    private String bloodType;
}
