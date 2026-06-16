package com.shanalert.hospitalalert.mapper;

import com.shanalert.hospitalalert.dto.EmergencyAlertResponse;
import com.shanalert.hospitalalert.entity.EmergencyAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmergencyAlertMapper {

    /**
     * Maps the new EmergencyAlert entity to a Response DTO.
     * Uses the builder pattern for clarity.
     */
    public EmergencyAlertResponse toDto(EmergencyAlert alert, String bedNumber, String message) {
        if (alert == null) return null;

        return EmergencyAlertResponse.builder()
                .id(alert.getId())
                .patientId(alert.getPatientId())
                .hospitalId(alert.getHospitalId())
                .status(alert.getStatus())
                .requestedBedType(alert.getRequestedBedType()) // Added to match new entity
                .estimatedArrivalTimeMinutes(alert.getEstimatedArrivalTimeMinutes())
                .patientNotes(alert.getPatientNotes())
                .admissionId(alert.getAdmissionId())
                .bedNumber(bedNumber != null ? bedNumber : "Unassigned")
                .message(message)
                .createdAt(alert.getCreatedAt()) // Now available from Auditable!
                .build();
    }
}