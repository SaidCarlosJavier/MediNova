package com.sistema.sistema_hospitalario.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalsResponse {
    private Double weight;
    private Double height;
    private String bp;
    private Double temp;
    private Integer hr;
    private Integer spo2;
}
