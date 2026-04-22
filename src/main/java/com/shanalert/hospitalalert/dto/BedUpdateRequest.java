package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import jakarta.validation.constraints.NotNull;


import java.util.UUID;


public record BedUpdateRequest(
        @NotNull(message = "Bed ID is required")
        UUID bedId,

//        String bedNumber,
        BedType bedType,
        BedStatus status
) {}