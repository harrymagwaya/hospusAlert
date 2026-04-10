package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.UUID;

@Builder
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

    // Critical for distance calculations
    private Double latitude;
    private Double longitude;

}