package com.shanalert.hospitalalert.mapper;

import com.shanalert.hospitalalert.dto.EmergencyAlertResponse;
import com.shanalert.hospitalalert.entity.EmergencyAlert;
import org.springframework.stereotype.Component;

@Component
public class EmergencyAlertMapper {

    /**
     * Maps the Alert entity and extra bed info into a clean DTO.
     * * @param alert The saved alert entity
     * @param bedNumber The human-readable bed name (e.g., ICU-01)
     * @param message A custom status message for the UI
     */
    public EmergencyAlertResponse toDto(EmergencyAlert alert, String bedNumber, String message) {
        if (alert == null) return null;

        return new EmergencyAlertResponse(
                alert.getId(),
                alert.getPatientId(),
                alert.getHospitalId(),
                alert.getAssignedBedId(),
                bedNumber,
                alert.getStatus(),
                alert.getEstimatedArrivalTimeMinutes(),
                message
        );
    }
}