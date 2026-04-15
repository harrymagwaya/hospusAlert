package com.shanalert.hospitalalert.dto;

import lombok.Data;

@Data
public class HospitalRequest {
    private String name;
    private String licenseNumber;
    private Integer icuBedsAvailable;
    private Boolean isEmergencyReady;
    private AddressRequest address; // The DTO with Lat/Long
}