package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.entity.Address;
import com.shanalert.hospitalalert.model.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientUpdateDTO {

    private LocalDate dateOfBirth;
    private String bloodGroup;
    private Double weightKg;
    private Double heightCm;

    // Medical Risk Profile
    private String allergies;
    private String chronicConditions;
    private String currentMedications;

    // Emergency Contact
    private String nextOfKinName;
    private String nextOfKinPhone;
    private RelationshipType nextOfKinRelationship;

    // Insurance
    private String insuranceProvider;
    private String insurancePolicyNumber;

    // Optional: Pass an address object to update location
    private AddressRequest address;
}