package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.EmergencyType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EmergencySearchRequest(
        @NotNull UUID patientId,
        @NotNull EmergencyType emergencyType,
        @NotNull Double patientLat,
        @NotNull Double patientLng,
        String patientNotes
) {}