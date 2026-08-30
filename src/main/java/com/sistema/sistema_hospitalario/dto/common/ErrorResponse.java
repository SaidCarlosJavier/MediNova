package com.sistema.sistema_hospitalario.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success;
    private int status;
    private String message;
    private String path;
    private Map<String, String> validationErrors;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
