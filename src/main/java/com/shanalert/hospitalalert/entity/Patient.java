package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.Gender;
import com.shanalert.hospitalalert.model.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Patient {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID id;

    // --- Authentication Fields ---
    @Column(unique = true, nullable = false)
    private String username; // For login (can be a handle)

    @Column(unique = true, nullable = false)
    private String email;

    private String firstName;
    private String lastName;

    @Column(length = 15)
    private String phoneNumber; // Critical for emergency SMS/Calls


    @Enumerated(EnumType.STRING)
    private UserRole role; //

    // --- Reusable Address Link ---
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(length = 5)
    private String bloodGroup; // O+, A-, etc.

    private Double weightKg; // Vital for emergency medicine dosages
    private Double heightCm;

    // --- Medical Risk Profile ---
    @Column(length = 1000)
    private String allergies; // High-priority for Triage

    @Column(length = 1000)
    private String chronicConditions; // Asthma, Diabetes, etc.

    @Column(length = 1000)
    private String currentMedications;

    // --- Emergency Contact / Next of Kin ---
    private String nextOfKinName;
    private String nextOfKinPhone;
    private String nextOfKinRelationship;

    // --- Insurance & Admin ---
    private String insuranceProvider;
    private String insurancePolicyNumber;
}
