package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.Auditable;
import com.shanalert.hospitalalert.model.HospitalStatus;
import com.shanalert.hospitalalert.entity.Address;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "hospitals")
public class Hospital extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String licenseNumber;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    @OneToMany(mappedBy = "hospitalId", cascade = CascadeType.ALL)
    private List<Bed> beds;

    @Enumerated(EnumType.STRING)
    private HospitalStatus status; //

    private Integer icuBedsAvailable;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isEmergencyReady = true;

    // Standard Getters/Setters
}