package com.sistema.sistema_hospitalario.dto.response;

import com.sistema.sistema_hospitalario.enums.AccountStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialtyResponse {

    private Long id;
    private String name;
    private String icon;
    private String description;
    private Integer doctorCount;
    private AccountStatus status;
}
