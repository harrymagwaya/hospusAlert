package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.HospitalStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class HospitalResponse {
    private UUID id;
    private String name;
    private String licenseNumber;
    private HospitalStatus status;
    private String city; // Flattened from the Address entity
    private Boolean isEmergencyReady;
}