package com.shanalert.hospitalalert.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HospitalPatientResponse(
        UUID patientId,
        UUID admissionId,
        String bedNumber,
        String bedType,
        String status,
        LocalDateTime admittedAt
) {}