package com.sistema.sistema_hospitalario.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequest {

    @NotBlank(message = "El nombre de la sede es obligatorio")
    private String name;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    private String phone;
    private List<String> rooms;
}
