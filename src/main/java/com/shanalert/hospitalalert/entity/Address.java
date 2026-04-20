package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.Auditable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "addresses")
public class Address extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String street;
    private String city;
    private String district; // e.g., Ntinda, Nakawa

    private String country;

    // Critical for distance calculations
    private Double latitude;
    private Double longitude;

}