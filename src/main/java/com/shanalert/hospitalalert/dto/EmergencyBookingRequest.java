package com.shanalert.hospitalalert.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EmergencyBookingRequest(
        @NotNull UUID alertId,
        @NotNull UUID hospitalId
) {}