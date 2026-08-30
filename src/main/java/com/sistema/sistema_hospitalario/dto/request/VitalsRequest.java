package com.sistema.sistema_hospitalario.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalsRequest {
    private Double weight;
    private Double height;
    private String bp;
    private Double temp;
    private Integer hr;
    private Integer spo2;
}
