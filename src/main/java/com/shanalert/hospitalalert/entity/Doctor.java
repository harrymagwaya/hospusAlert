package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Doctor extends Auditable {
    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID id;

    // --- Authentication Fields ---
    @Column(unique = true, nullable = false)
    private String username; // For login (can be a handle)

    @Column(unique = true, nullable = false)
    private String email;

    // --- Basic Identity Fields ---
    private String firstName;
    private String lastName;

    private Gender gender;

    @Column(length = 15)
    private String phoneNumber; // Critical for emergency SMS/Calls

    @Enumerated(EnumType.STRING)
    private UserRole role; //

    // --- Professional Credentials ---
    @Column(unique = true, nullable = false)
    private String medicalLicenseNumber;

    @Enumerated(EnumType.STRING)
    private MedicalSpecialty specialization;

    @Column(length = 500)
    private String qualifications;

    private Integer yearsOfExperience;


//    private UUID hospitalId; // Links to the Hospital entity



    private String department; // e.g., "Accident & Emergency (A&E)"

    @Builder.Default
    // --- Operational Status (Triage Logic) ---
    private Boolean isAvailable = true; // Can they accept a new patient right now?

    @Enumerated(EnumType.STRING)
    private DoctorStatus status; //

    // --- Emergency Preferences ---
    private Boolean handlesTrauma;
    private Boolean handlesPediatrics;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "doctor_hospitals",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "hospital_id")
    )
    private List<Hospital> hospitals = new ArrayList<>();

}
