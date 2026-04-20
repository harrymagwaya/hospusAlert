package com.shanalert.hospitalalert.dto;


import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;

import java.util.UUID;

public record BedResponse(
        UUID bedId,
        String bedNumber,
        BedType bedType,
        BedStatus status,
        String message
) {}