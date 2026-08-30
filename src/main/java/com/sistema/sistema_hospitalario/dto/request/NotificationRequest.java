package com.sistema.sistema_hospitalario.dto.request;

import com.sistema.sistema_hospitalario.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    @NotNull(message = "El usuario es obligatorio")
    private Long userId;

    private String title;
    private String message;
    private NotificationType type;
}
