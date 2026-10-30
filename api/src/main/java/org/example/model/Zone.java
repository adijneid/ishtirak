package org.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "zone")
@Getter
@Setter
public class Zone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long zone_id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "is_rural", nullable = false)
    private boolean is_rural;
}
