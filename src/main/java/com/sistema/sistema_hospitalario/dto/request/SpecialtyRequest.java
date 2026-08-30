package com.sistema.sistema_hospitalario.dto.request;

import com.sistema.sistema_hospitalario.enums.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyRequest {

    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    private String name;

    private String icon;
    private String description;
    private AccountStatus status;
}
