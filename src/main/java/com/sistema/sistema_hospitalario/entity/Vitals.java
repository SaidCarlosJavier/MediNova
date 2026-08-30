package com.sistema.sistema_hospitalario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vitals {

    private Double weight;

    private Double height;

    @Column(name = "blood_pressure")
    private String bp;

    private Double temp;

    @Column(name = "heart_rate")
    private Integer hr;

    private Integer spo2;
}
