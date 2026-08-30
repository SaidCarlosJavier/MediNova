package com.sistema.sistema_hospitalario.dto.response;

import com.sistema.sistema_hospitalario.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String name;
    private String email;
    private Role role;
}
