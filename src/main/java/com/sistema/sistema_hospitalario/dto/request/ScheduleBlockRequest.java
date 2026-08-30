package com.sistema.sistema_hospitalario.dto.request;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleBlockRequest {
    private String day;
    private LocalTime from;
    private LocalTime to;
}
