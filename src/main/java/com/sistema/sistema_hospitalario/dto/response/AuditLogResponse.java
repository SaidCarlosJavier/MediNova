package com.sistema.sistema_hospitalario.dto.response;

import com.sistema.sistema_hospitalario.enums.AuditModule;
import com.sistema.sistema_hospitalario.enums.AuditStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private String user;
    private String action;
    private AuditModule module;
    private LocalDate date;
    private LocalTime time;
    private AuditStatus status;
}
