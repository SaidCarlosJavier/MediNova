package com.sistema.sistema_hospitalario.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponse {

    private Long id;
    private String name;
    private String address;
    private String phone;
    private List<String> rooms;
}
