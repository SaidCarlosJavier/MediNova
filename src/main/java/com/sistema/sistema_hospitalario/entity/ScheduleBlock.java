package com.sistema.sistema_hospitalario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.time.LocalTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleBlock {

    @Column
    private String day;

    @Column(name = "from_time")
    private LocalTime from;

    @Column(name = "to_time")
    private LocalTime to;
}
