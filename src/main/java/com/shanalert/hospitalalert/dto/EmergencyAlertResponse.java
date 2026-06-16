package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.AlertStatus;
import com.shanalert.hospitalalert.model.BedType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder // Standard builder is fine if it doesn't extend Auditable
@AllArgsConstructor
@NoArgsConstructor
public class EmergencyAlertResponse {
    private UUID id;
    private UUID patientId;
    private UUID hospitalId;
    private AlertStatus status;
    private BedType requestedBedType;
    private Integer estimatedArrivalTimeMinutes;
    private String patientNotes;
    private UUID admissionId;
    private String bedNumber;
    private String message;
    private LocalDateTime createdAt;
}