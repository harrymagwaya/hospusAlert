package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.AlertStatus;
import java.util.UUID;


public record EmergencyAlertResponse(
        UUID alertId,
        UUID patientId,
        UUID hospitalId,
        UUID assignedBedId,
        String bedNumber,                // From the Bed entity
        AlertStatus status,              // PENDING, RESPONDING, etc.
        Integer estimatedArrivalTimeMinutes, // Calculated via OSRM
        String message                   // Human-readable status for UI
) {}