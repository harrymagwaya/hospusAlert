package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.FacilityLevel;
import com.shanalert.hospitalalert.model.HospitalStatus;
import com.shanalert.hospitalalert.model.OwnershipType;
import lombok.Data;

@Data
public class HospitalRequest {
    private String name;
    private String licenseNumber;
    private Integer icuBedsAvailable;
    private Boolean isEmergencyReady;
    private AddressRequest address; // The DTO with Lat/Long
    private OwnershipType ownershipType;
    private FacilityLevel facilityLevel;
    private HospitalStatus status;
}