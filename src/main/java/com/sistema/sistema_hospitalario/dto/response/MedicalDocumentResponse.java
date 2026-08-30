package com.sistema.sistema_hospitalario.dto.response;

import com.sistema.sistema_hospitalario.enums.DocumentStatus;
import com.sistema.sistema_hospitalario.enums.DocumentType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalDocumentResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String name;
    private DocumentType type;
    private LocalDateTime date;
    private String size;
    private DocumentStatus status;
}
