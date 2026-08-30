package com.sistema.sistema_hospitalario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private String address;

    private String phone;

    @ElementCollection
    @CollectionTable(name = "location_rooms", joinColumns = @JoinColumn(name = "location_id"))
    @Column(name = "room")
    @Builder.Default
    private List<String> rooms = new ArrayList<>();
}
