package com.sistema.sistema_hospitalario.dto.response;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleBlockResponse {
    private String day;
    private LocalTime from;
    private LocalTime to;
}
