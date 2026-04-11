package com.shanalert.hospitalalert.dto;

import java.util.UUID;

/**
 * Data returned to the Patient App during the "Hospital Selection" phase.
 * Includes geographic data for mapping and ETA for decision making.
 */
public record HospitalDiscoveryResponse(
        UUID hospitalId,
        String hospitalName,
        String streetAddress,
        Integer estimatedMinutes, // Calculated via OSRM
        Integer availableBeds,     // Number of available beds of the requested type
        Double latitude,           // For Google Maps/OSM plotting
        Double longitude
) {}