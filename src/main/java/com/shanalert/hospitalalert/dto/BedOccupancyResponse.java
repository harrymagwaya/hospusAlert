package com.shanalert.hospitalalert.dto;

import java.util.UUID;

public record BedOccupancyResponse(
        UUID bedId,
        String bedNumber,
        String bedType,
        String bedStatus,

        UUID patientId,
        String patientName,

        String admissionStatus
) {}