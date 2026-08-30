package com.sistema.sistema_hospitalario.dto.response;

import com.sistema.sistema_hospitalario.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private LocalDateTime date;
    private boolean read;
}
