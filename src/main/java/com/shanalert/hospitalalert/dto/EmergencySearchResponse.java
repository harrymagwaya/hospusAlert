package com.shanalert.hospitalalert.dto;

import java.util.List;
import java.util.UUID;

public record EmergencySearchResponse(
        UUID alertId,
        List<HospitalDiscoveryResponse> hospitals
) {}