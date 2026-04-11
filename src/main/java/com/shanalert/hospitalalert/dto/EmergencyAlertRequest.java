package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.BedType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EmergencyAlertRequest(
        @NotNull UUID patientId,
        @NotNull UUID hospitalId,
        @NotNull BedType requestedBedType,
        @NotNull Double patientLat,
        @NotNull Double patientLng,
        String patientNotes
) {}