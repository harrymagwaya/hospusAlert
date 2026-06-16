package com.shanalert.hospitalalert.dto;


import com.shanalert.hospitalalert.model.BedType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;


public record BedCreateRequest(

        @NotNull(message = "Please specify the department (Bed Type)")
        BedType bedType,

        @Min(value = 1, message = "You must provide at least one bed")
        int totalCount
) {}