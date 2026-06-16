package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.BedType;

import java.util.UUID;

public record BedCreateResponse(
        UUID hospitalId,
        BedType bedType,
        Integer requestedTotal,
        Integer previousCount,
        Integer createdCount,
        Integer finalCount,
        String message
) {
}