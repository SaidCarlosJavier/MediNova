package com.sistema.sistema_hospitalario.dto.request;

import com.sistema.sistema_hospitalario.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalDocumentRequest {

    @NotNull(message = "El paciente es obligatorio")
    private Long patientId;

    private Long doctorId;

    @NotBlank(message = "El nombre del documento es obligatorio")
    private String name;

    private DocumentType type;
    private String size;
}
